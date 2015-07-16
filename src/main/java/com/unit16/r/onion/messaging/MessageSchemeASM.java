package com.unit16.r.onion.messaging;

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_6;

import java.util.HashMap;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class MessageSchemeASM extends MessageScheme {
	
	public static void init() {
		String.valueOf(new Generator());
	}
	
	/** class loader used to load byte code as classes on the fly */
	private static final class Generator extends ClassLoader {
		final static Generator instance = new Generator();
		Generator() {
			super(MessageSchemeASM.class.getClassLoader());
		}
		static Class<?> load(String className, byte[] code) {
			return instance.defineClass(className, code, 0, code.length);
		}
	}
	
	private static Logger log = LoggerFactory.getLogger(MessageSchemeASM.class);
	private static int count = 1;
	private static HashMap<String, Class<?>> dispatchers = new HashMap<>();
	
	public MessageSchemeASM(String name) {
		super(name);
	}

	@Override
	protected <T> Dispatcher buildDispatcher(final Source<T> source, final Object dest, final Dispatcher next) {
		
		try {
			
			final String destName = dest.getClass().getName().replace('.', '/');
			final String method = "handle";
			final String objectName = source.type.getName().replace('.', '/');

			final String dispName = destName + "." + method + objectName;
			
			final Class<?> klass;
			// reuse existing class (because if I don't, we leak memory. Java doesn't like
			// loading 30'000 klasses ...
			if(dispatchers.containsKey(dispName)) {
				log.debug("Reusing existing dispatcher {}", dispName);
				klass = dispatchers.get(dispName);
			}
			// create new dispatcher
			else {
				log.debug("Creating new dispatcher {}", dispName);
				final String klassNameOrig =  String.format("%s$DispatcherImpl#%d_handle(%s)",
					dest.getClass().getName(),
					count++,
					source.type.getSimpleName());
				final String klassName = klassNameOrig.replace('.', '/');
				
				final String dispBaseName = "com/unit16/r/onion/messaging/Dispatcher";
				final String dispImplName = dispBaseName + "Impl";			
				
		        ClassWriter cw = new ClassWriter(0);
		        MethodVisitor mv;
		
		        cw.visit(V1_6, ACC_PUBLIC + ACC_SUPER, klassName, null, dispImplName, null);	        
		        {
		            mv = cw.visitMethod(ACC_PUBLIC, "<init>", "(Ljava/lang/Object;L" + dispBaseName + ";)V", null, null);
		            mv.visitCode();
		            mv.visitVarInsn(ALOAD, 0);
		            mv.visitVarInsn(ALOAD, 1);
		            mv.visitVarInsn(ALOAD, 2);
		            mv.visitMethodInsn(INVOKESPECIAL, dispImplName, "<init>", "(Ljava/lang/Object;L" + dispBaseName + ";)V");
		            mv.visitInsn(RETURN);
		            mv.visitMaxs(3, 3);
		            mv.visitEnd();
		        }
		        {	        	
		            mv = cw.visitMethod(ACC_PUBLIC, "dispatch", "(Ljava/lang/Object;)V", null, null);
		            mv.visitCode();         
		            // load dest
		            mv.visitVarInsn(ALOAD, 0);
		            mv.visitFieldInsn(GETFIELD, dispImplName, "dest", "Ljava/lang/Object;");
		            mv.visitTypeInsn(CHECKCAST, destName);
		            mv.visitVarInsn(ALOAD, 1);
		            mv.visitTypeInsn(CHECKCAST, objectName);
		            // invoke handle
		            mv.visitMethodInsn(INVOKEVIRTUAL, destName, method, "(L" + objectName + ";)V");
		            // dispatch to next	           
	                mv.visitVarInsn(ALOAD, 0);
	                mv.visitFieldInsn(GETFIELD, dispBaseName, "next", "L" + dispBaseName + ";");
	                mv.visitVarInsn(ALOAD, 1);
	                mv.visitMethodInsn(INVOKEVIRTUAL, dispBaseName, "dispatch", "(Ljava/lang/Object;)V");                          
		            mv.visitInsn(RETURN);
		            mv.visitMaxs(2, 2);
		            mv.visitEnd();
		        }
		        cw.visitEnd();		        
		        klass = Generator.load(klassNameOrig, cw.toByteArray());
		        dispatchers.put(dispName, klass);
			}
	        return (DispatcherImpl)klass.getConstructor(Object.class, Dispatcher.class).newInstance(dest, next);
	        
		} catch(Exception e) {
			throw new IllegalArgumentException("Failed to connect " + source + " and " + dest 
					+ ": " + e.getMessage(), e);
		}
	}
	
}

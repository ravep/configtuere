package com.unit16.common.logback;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

public class SMTPAppender extends ch.qos.logback.classic.net.SMTPAppender {
    protected String replyTo;

    // copied from SMTPAppenderBase.java
    // this is package-private instead of protected =\
    protected InternetAddress getAddressCopy(String addressStr) {
        try {
            return new InternetAddress(addressStr);
        } catch (AddressException e) {
            addError("Could not parse address [" + addressStr + "].", e);
            return null;
        }
    }


    @Override
    public void start() {
        super.start();
        if(mimeMsg != null && replyTo != null) {
            try {
                mimeMsg.setReplyTo(new InternetAddress[]{getAddressCopy(replyTo)});
            } catch(MessagingException e) {
                addError("Could not activate SMTPAppender options.", e);
            }
        }
    }

    public String getReplyTo() {
        return replyTo;
    }

    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }

}



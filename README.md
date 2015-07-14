# ponfig
A lean but handy self-documenting and validating configuration system.

How does it work?

1) Define a group in a class A together with two variables within that group

public final static GroupDef CONF = GroupDef.create("master").build();
public final static C<Integer> PARAM1 = CONF.integer("param1").build();
public final static C<String> PARAM2 = CONF.str("param_two").build();




 






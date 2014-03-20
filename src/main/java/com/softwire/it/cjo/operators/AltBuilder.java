package com.softwire.it.cjo.operators;

/**
 * ****************<br>
 * Date: 20/03/2014<br>
 * Author:  michael<br>
 * ****************<br>
 * <br>
 * This class forms part of the builder pattern. Alts have a large number of possible arguments,
 * and putting them all into the same constructor is infeasible (or at least very confusing). Instead,
 * you set all of the various components of the Alt inside a builder, and then get the item from the builder
 * when you're done.<br>
 * <br>
 * An important implementation note: guards for branches on alts are evaluated immediately before
 * any locks are required and the channels waited on. They are evaluated sequentially in the order
 * in which they were added. If parallelism is required or efficient, you can arrange
 * to do this computation in the first guard - it is not the concern of the operator.<br>
 * In general, guards should be quick to evaluate. Make sure you cannot trigger deadlock according to the above rule.
 * <br>
 * Given that thread safeness is quite a big deal in an application like this, I've decided to make this 
 * thread safe by ensuring the builder returns immutable objects. That is, the builder itself is immutable,
 * and each method returns another builder with the new values set.
 * 
 */
public class AltBuilder {
	//We store the list of guards
}

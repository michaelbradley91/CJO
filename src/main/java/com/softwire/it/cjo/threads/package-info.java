/**
 * ****************<br>
 * Date: 21/03/2014<br>
 * Author:  michael<br>
 * ****************<br>
 * <br>
 * This package contains all classes related to the spawning of additional threads in the application.<br>
 * CJO makes use of its own thread spawner when you use its operators (acts like a cached thread pool).<br>
 * This won't affect any other threads spawned ordinarily in an application (although it is of course recommended
 * you don't mix and match between concurrency handling techniques...)
 *
 */
package com.softwire.it.cjo.threads;
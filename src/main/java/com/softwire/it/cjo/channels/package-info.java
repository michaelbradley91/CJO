/**
 * ****************<br>
 * Date: 15/03/2014<br>
 * Author:  michael<br>
 * ****************<br>
 * <br>
 * This package contains all of the basic channels provided by CJO to enable concurrency!<br>
 * This package does not include operators, such as alt, or serve or parallel composition.<br>
 * <br>
 * All channels have two different ends - a read end where you extract the message and a write end where you put the message in.<br>
 * Channels vary in how many processes are permitted to synchronise on a channel at once, and whether or not the writing
 * is asynchronous. It's up to you to choose those best suited for the task!
 * 
 */
package com.softwire.it.cjo.channels;
/**
 * Notes to self:
 * 
 * How should channels be implemented???
 * We've already decided that channels are going to be resources in the resource graph. Things like alts
 * are also resources, that have dependencies on the channels.
 * 
 * When I want to write to a channel, I can lock onto it and be sure that I am aware of all the readers/writers.
 * Depending on the situation, I may interact in a variety of ways, but what's important is that we can release a reader if a reader
 * is available, or wait otherwise.
 * 
 * The simplest way to manage this is to construct a semaphore, pull it down, then release the resource, and then pull down on the
 * semaphore. If a reader doesn't find me, I'll just pull down and wait until I'm released. If a reader does find me, even before
 * I've started pulling down, I'm still guaranteed to go through - of course, I might not see the message.
 * 
 * So, for a reader who may need to wait, I guess it makes sense to leave behind both a box and a semaphore. The writer
 * can put the message inside the box strictly before releasing me. Then I'll get it when I wake up.
 * 
 * A slight issue with this... firstly, it is using quite a lot of memory, and having attempted these things
 * with monitors, it isn't really necessary for everyone to have a separate semaphore...
 * 
 * The major issue is that this doesn't seem to play well with alts. If I am waiting on two channels, do I leave the same semaphore
 * for both, and how do I de-register my interest?? I'd rather not have to enforce that everyone behaves specially for alts
 * when they see them.
 * 
 * Instead, I'd rather the waiting process enabled the process its waking up to respond in some way.
 * A simple way to do this, which also removes the needs for specific semaphores, is to pass
 * a function to be run when the process is woken up. (It's assumed due to the resource graph that all relevant
 * components are locked).
 * 
 * This function had better take the message in the case of a writer, but it should also know who exactly the reader is.
 * For the purposes of an alt, this is especially important as it will want to know which branch was released.
 * How can the branch be known? - the resource is unique, so just passing the channel in itself should be sufficient.
 * 
 * The alt, when its method is called, can subsequently de-register its interest in all other channels
 * that it was listening too, which again are guaranteed not to have made an attempt to respond due to the wonders
 * of the resource graph. :)
 * 
 * We can implement fairness fairly easily, just by storing these requests as lists of functions.
 * To be honest, we'll just pass the channel in to make up the list, so that the functions have proper names.
 * 
 * If it is a list, it is a pain to have to remove random items.
 * We can avoid this if the item is stored alongside a flag to say whether or not it is still
 * interested (or specifically, that occasion was still interested).
 * So, we'd probably use a pair of a box for the flag to specify interest, and the channel itself.
 * Handling the correct boxes shouldn't be too hard, even for alts.
 * A cleaner way for the sake of the garbage collector is to store the list as a doubly linked list,
 * and then when removal is scheduled we can manage this in O(1) (with a hash map if life is that restricting,
 * but for speed I think we can just return an identity that should be used)
 * 
 * With an alt containing guards, we can just evaluate the guards immediately once all the resources are locked, and throw
 * away the resources we didn't need once set up (by not adding dependencies)
 * For the orelse, we simply activate this if all guards fail. (We could evaluate guards asynchronously, which is more efficient
 * so probably worth it...?)
 * For the "after" statement, we can spawn a separate thread to wait for the required amount of time, and then just try to lock
 * on the alt's resource, and see if it activated since its specific creation (the alt will probably set something inside
 * it to tell it not to worry).
 * 
 * A more awkward issue is how to handle interrupts. It would be nice for our channels not to wait on each other uninterruptibly.
 * So, when a channel is woken up by an interrupt when it had been waiting on its semaphore, the easiest thing to do is to try locking
 * onto its resource, check if the communication was successful anyway (and if so don't bother killing it), but otherwise de-register
 * interest and pass it down...?
 * 
 * A nuisance about passing it down is that you would need loads of try catch statements around your
 * channels. This is maybe no unreasonable, and we could add "uninterruptibly" versions of each method, but that is seriously
 * painful, and honestly, probably a bad plan... (would probably be better to include uninterruptible as a flag based
 * on what we're about to suggest)
 * 
 * I'd like to convert interruptions to runtime exceptions, so that they don't need loads of try catch statements necessarily.
 * Honestly, there's rarely a good idea as to what to do in situations like this anyway, so I'm pretty happy with this plan
 * and it technically provides the same power (although it is a lot less clear what's going on).
 * 
 * For other kinds of exceptions, we'll just have to throw them I suppose. I need to be careful
 * to catch them first, and handle the situation gracefully nevertheless.
 * 
 * For operators like parallel composition, how the exception is thrown is kind of important, but we won't worry here...
 * 
 * Something enabled in CSO is to close channels. Is this worth it?
 * In terms of implementation, it is a matter of locking on the channel, closing it, and then throwing out all listeners
 * accordingly... Any further attempts to listen would receive the same kind of response...
 * I guess it isn't particularly burdensome to implement. It was used as a termination mechanism in CSO,
 * so I'd quite like it. There's no reason why we can't allow channels to be re-opened like this too.
 * 
 * There's a bit of ambiguity as to what an alt should do if one its channels is closed... does it wait for the others or stop
 * immediately?? 
 * 
 * A listener which is thrown out by a channel is meant to throw a "Stop exception" - a special kind of exception to indicate
 * the channels were intentionally closed - so this isn't so much a failure but an understanding of termination.
 * 
 * So... channels had better support the following I guess...
 * "Add read listener" (throws closed exception)
 * "Add write listener" (throws closed exception)
 * "Remove read listener"
 * "Remove write listener"
 * "Close"
 * "Close read end"
 * "Close write end"
 * 
 * In terms of the closein and closeout business... It is kind of nice being able to separate the read and write
 * components of the channel. For the sake of abstraction, providing access to only one half each is nice, but the implementation
 * needs to be aware of both...
 * The closein and closeout vary according to the channels flexibility on in and out. closein does nothing on a many-many channel!
 * Honestly, I don't like this... To enforce the contract faithfully, I could try to keep track of threads, but that's dangerous due
 * to the scheduler.
 * 
 * I think I'd prefer to drop this notion. Just have a single close that kills everything...? Maybe not...
 * For certain processes which aren't aware of their context, they'd like to just closein or closeout, so that other writers
 * or readers also using the channel might still be able to continue...
 * 
 * According to the online notes, the informal contract (where we imagine a 1-1 channel to be owned by just one process) is:
 * close - promises no process with ever read or write to this channel again
 * closein - promises that this process will never read from this channel again
 * closeout - promises that this process will never write to this channel again
 * 
 * I don't think we need open - it's probably bad practice anyway, and I already don't like this area...
 * In the old CJO attempt, there was a way to close all possible in, but allow writers and readers already inside to complete
 * their interactions before the channel actually closed. In other words, writers and readers would not necessarily
 * be thrown out unless there was no one else to interact with. 
 * Given the way closein and closeout will actually work, I don't think there's any use to this... (we can't have transactions
 * in mid process anymore since the invention of the resource graph)
 * 
 * Depending on the channel, when the listener is added if the writer or otherwise is added it will trigger a sequence of interactions...
 * The listeners should support
 * "Reader arrived" (with the reader itself)
 * "Writer arrived" (with message + the writer itself)
 * 
 * These methods are all named in helpful ways for the implementation, but are a bit weird for the user.
 * Since the implementation is pretty complicated, I'm quite tempted just to wrap up the method names
 * with a surrounding class... We'll see, but for now I'm going with helpful names.
 * 
 * We do need a naming convention. Do we use read or write, or in and out??
 * I never really liked in and out. You can read from in and write to out, but you push messages in when you write, and take
 * them out when you read!!? I don't want to clash with the convention in CSO, as that will probably confuse people
 * even more...
 * 
 * Also, Java's kind of harsh in what method names we can use (no operator overloading). We'll probably just use "read" and "write" for channels,
 * rather than ? and !. So, maybe I should call the two ends of a channel source and target or something... that would probably
 * only make sense to people with some graph theory.
 * 
 * I could call them "writeEnd" and "readEnd" which is particularly clear, but quite cumbersome...
 * or "readChannel" and "writeChannel" since the two combined form a channel. Also writeEnd doesn't make it clear it's a channel.
 * I could go with "ChannelReader" and "ChannelWriter" to make it clear that one allows writing to channels, and the other
 * reading from channels. These are probably the best clear names...
 * 
 * I think that's how it is a going to work...
 * 
 * Trying to determine the typing for classes...
 * 
 * All classes need these standard methods in their implementation, but it is a problem as I want to hide
 * these methods from the user. Granted, I could just type cast everything, it is not very OOP...
 * 
 * I guess I'll have to make the classes abstract. ok - since Java doesn't support multiple extensions,
 * I can't do it directly this way either... The problem is that generally knowing the channel is a reader
 * or a writer is not enough for an alt - it needs to know the underlying implementation access methods.
 * 
 * This means the interfaces could do with protected methods, but this isn't possible either in Java.
 * The "best" I can do seemingly is to use an abstract class called "channel" which hides the implementation specific
 * methods, and then operators will typically cast channel readers and writers to channels...
 * 
 * To avoid the casting, I can enable the use of a method getChannel underneath the channel - not exactly
 * brilliant, but since everyone knows it can be cast, I guess it is just better to be clear about it
 * (I don't want later implementations of readers or writers not to be channels - so I wanted to avoid relying on this)
 * 
 * An alternative:
 * 
 * This is quite painful... however, I can perform the following mechanism:
 * I can enable the user of a channel to get both the read and the write ends of a channel.
 * We can then make both abstract, and use a protected method to get hold of he original channel again.
 * 
 * In the implementation, the methods would basically be forwarded. It is a bit of a pain for the user,
 * as they have to constantly get the correct end of the channel... but I guess it should work...
 * (Prevents bad casting too!)
 * 
 */
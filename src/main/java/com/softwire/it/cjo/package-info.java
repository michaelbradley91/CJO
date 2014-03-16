/**
 * ****************
 * Date: 10/03/2014
 * @author michael
 * ****************
 * 
 * This project is designed to replicate the CSO library in Scala, hopefully with a few bugs removed.
 * 
 * CJO = Communicating Java Objects. This project should enable future applications to implement
 * parallel processing via "channels", a high level abstraction for processes synchronising with each other
 * via sending messages down channels/pipes. It is based on the Concurrent Programming Course.
 * 
 * See specific classes for their own theoretical contribution.
 *
 */
package com.softwire.it.cjo;
/**
 * Notes to self:
 * 
 * These are notes for me to use when trying to build a particularly complicated part of the project.
 * 
 * Problem 1: resource sharing between alts
 * 
 * I tried to implement this a while ago on Windows, and came up against quite a few major problems. The largest of all
 * was the fact that different alts (tools to wait for any of a number of channels to become active) could interact with
 * each other, and effectively create a hierarchy of locking synchronisations... In order to start listening to an alt,
 * you would need to lock on the channels in that alt, then on the other channels in alts related to any of the alts
 * in the first alt, etc... very confusing!
 * 
 * I didn't realise alts were supposed to be able to interact at the time, so trying to bolt this on was
 * extremely dangerous. I left it in a state where it "might" have been working, but good luck proving it!
 * Either way, the code's complexity and delicacy made it an unsatisfactory attempt... so we're back!
 * 
 * Sorting out how to lock on the appropriate resources is something I want to get right early on,
 * as changing this later could destroy the world (literally if it were deployed in a major application! Who knows...).
 * 
 * The plan:
 * 
 * Alts linking together reminds a lot of graphs and edges. The idea is that each channel, upon creation,
 * becomes a vertex on the graph. When you want to interact with a channel, you must lock on all vertices in the graph
 * that the channel's vertex is connected to.
 * 
 * When an alt is included, it will be a vertex of its own, and it will have edges added to it to connect its vertex
 * to all other vertices/channels inside the alt.
 * 
 * Locking in order however, is a big problem. To add an edge to the graph, it is important to lock onto the source and target
 * vertices (we'll ignore channels now as edges can even exist between alts - nested alts). Without an ordering,
 * we might deadlock with another thread trying to do the same thing.
 * 
 * To simplify, we will always ensure that every connected component of the graph has exactly one representative,
 * and only the representative's lock needs to be obtained to control everything it is connected to. Each representative
 * will have some kind of id as a long or preferably (to avoid overflow) a Big Integer. When trying to lock between
 * two groups, you will generally try to lock on the group with the smaller id first, and then the other.
 * (Note that unless we restrict ourselves to binary alts, it may be necessary to lock onto more than two groups)
 * 
 * When groups are merged the representatives will change and so must the ids. If I have a lock on one group
 * that I wanted to merge with others, if the second group's id falls below mine I might trigger deadlock unless I release the lock!!
 * This is obviously bad, but we can ensure that this is impossible by maintaining that whenever a group on the graph changes/is created,
 * the id always increases.
 * 
 * The next issue is what to do when I am waiting to lock in the right order...
 * It might be the case that the id was lowest when I started waiting for the lock, but when I obtain the lock,
 * it could have risen and now be higher than a different group I wanted to lock on at the same time. Does this put
 * us back where we started?
 * 
 * Obviously we can't keep holding onto the group we obtained out of order. A simple solution is to just throw away the lock and start again.
 * It'll work eventually for at least somebody, but fairness is quite a big problem in this setup! It might be possible to handle fairness in some more complicated way
 * later...
 * 
 * Another issue is if the groups we needed to lock onto change altogether!! This is all handled by the following protocol (which could run forever without
 * some kind of seriously complicated fairness...)
 * 
 * 0. Scan for all the groups I need to lock onto asynchronously. In order, call the groups (A,B,C...)
 * 1. Wait for the lock on A's representative
 * 2. Once obtained, check A "still exists" as it might have been deleted as a representative. If it doesn't, release the lock and repeat from 0.
 * 3. If A does exist, see if its id changed. If it did change, asynchronously scan the groups you need to merge with. If its id is still
 * the smallest, proceed back to 0. to get the next group. Otherwise, release the lock and repeat from 0.
 * 4. If A's id hasn't changed, go to 0. and obtain a lock on the next group.
 * 
 * The above cannot deadlock: since the ids only increase, it will only hold onto a group when it is sure all remaining groups it might ever want will have ids
 * strictly larger than the one it has. However, the above can be forced to restart a LOT if some complicated stuff is going on...
 * 
 * The asynchronous scan only benefits the algorithm because ids only increase - it establishes some lower bounds on ids. Re-scanning
 * groups each time you obtain one is always mandatory, as the structure could have changed dramatically and you may not even need any further groups!!
 * 
 * We'll also assume we keep the ids of groups unique, so that the scanner can check to see if certain channels appear in the same
 * group just by seeing "asynchronously" if they have the same id (i.e: once it has a lock on one of their groups).
 * 
 * Is there a better way to do this (that doesn't require restarts, or where fairness is a lot easier to enforce...)?
 * 
 * Doesn't seem like it...
 * 
 * Alts don't nest!! This is quite important because it is useful for fairness to enforce that representatives can pass around
 * a queue of processes waiting on them to each other, and if the representative being waited on ceased to exist, it would be kind
 * of awkward to tell everyone who's been queueing... Without nested alts, the program can only be asking for the representatives
 * of specific channels, which are largely persistent.
 * 
 * Ughh... type restrictions and access modifier restrictions are becoming a big pain...
 * The only way to get the official readers or writers inside is to use a public interface...
 * I guess having a public interface isn't such a disaster... as nothing else will be using it anyway... things
 * like pair and function are already visible (kind of unfortunately), so I guess its ok... frustrating though...
 * Otherwise, everything has to live in the same package which I'd say sucks even more (we could reorganise at the end
 * if desperate...
 * 
 */
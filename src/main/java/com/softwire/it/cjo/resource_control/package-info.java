/**
 * ****************<br>
 * Date: 14/03/2014<br>
 * Author:  michael<br>
 * ****************<br>
 * <br>
 * This package contains all of the classes involved in managing resources concurrently.<br>
 * Specifically, the resources to be controlled are the channels. It is important to lock onto
 * the right groups of channels to prevent deadlock! This turns out to be difficult...
 * <br>
 * A resource will form part of a graph. Each resource is a vertex, and every other resource that
 * depends on it will be connected to it by an edge (or by any other path through the graph).<br>
 * <br>
 * Connected components of a graph form one abstract resource, and all must be locked to manipulate any.<br>
 * Resources can be added atomically, removed etc. Actual modifications to the graph should be applied through a resource manipulator
 * (which enforces the atomicity of actions)
 *
 */
package com.softwire.it.cjo.resource_control;
/**
 * Notes to self:<br>
 * <br>
 * Finally worked out how to implement this efficiently, and correctly as far as I can tell without testing (coming soon!)<br>
 * <br>
 * The resources are added together using a disjoint set forest data structure, but without union by rank (which is still O(n*log(n))).<br>
 * Union by rank could be added, but at a significant overhead, so I didn't consider it worthwhile (you can't just add the integers - in a union
 * the representatives must not be allowed to decrease for any resource)<br>
 * <br>
 * The resources can be removed without having disconnected vertices updated at first, but this can be corrected for at the end.<br>
 * The resource manipulator keeps track of which vertices may be out of date, and then can trigger a final update with a one time
 * scan afterwards (using a bfs style algorithm).<br>
 * <br>
 * To avoid synchronising globally within representatives, but having a desired ordering and ensuring that representative's of resources
 * only ever increase, (the minimum requirement - new ones can be introduced with lower values, but these can't be merged on late by the algorithm)
 * each new rep is given a base id by its manipulator, ensuring it is higher than all representatives owned by that manipulator. Hence, if it assigned
 * to a resource, the value will only increase.<br>
 * The reps may get the same baseid, so they add a random component to break up ties. This is fixed as well so it doesn't have to be synchronised on.<br>
 * In the event the random component fails, the reps will synchronise on their own id (so local to them) and start adding further random numbers
 * to break the tie, succeeding with higher probability than your computer not exploding.<br>
 * <br>
 * The graph itself is entirely held by the resources, and not globally by the resource graph. This means a resource is never intentionally destroyed,
 * but it can have its edges removed and will presumably be garbage collected eventually.<br>
 * <br>
 * So, there are no bottlenecks!!! Woo! Fairness is ensured by each process keeping an eye on how many restarts it has endured, and if this rises
 * above a certain threshold, it will stop all other processes from entering until it and all other old processes have left - thus ensuring it
 * will succeed eventually!<br>
 * <br>
 * All other synchronisation is managed by fair semaphores, so fairness is truly guaranteed (as much as a scheduler ever permits).
 * 
 */
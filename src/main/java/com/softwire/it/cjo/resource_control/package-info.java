/**
 * ****************
 * Date: 11/03/2014
 * @author michael
 * ****************
 * 
 * This package contains all of the classes involved in managing resources concurrently.
 * Specifically, the resources to be controlled are the channels. It is important to lock onto
 * the right groups of channels to prevent deadlock! This turns out to be difficult...
 * 
 * A resource will form part of a graph. Each resource is a vertex, and every other resource that
 * depends on it will be connected to it by an edge (or by any other path through the graph).
 * 
 * Connected components of a graph form one abstract resource, and all must be locked to manipulate any.
 * Resources can be added atomically, removed etc. Actual modifications to the graph should be applied through a resource manipulator
 * (which enforces the atomicity of actions)
 *
 */
package com.softwire.it.cjo.resource_control;
/**
 * Notes to self:
 * 
 * Managing the representatives can be surprisingly painful!!! It appears the following is the best I can do:
 * A simplification which seems to be necessary is that if the id of a representative changed, it is better to assume the representative
 * was destroyed... This could mean even more restarts, but getting around it appears very difficult!!!
 * 
 * Representatives have pointers to parent representatives in case they no longer apply but the parent is the new representative.
 * When a node is queried for its representative, the real representative is passed down, and it updates its reference.
 * During a split, it is necessary to perform a depth first forest like search to identify all of the connected components, assigning
 * them new representatives at a "flat" level in the hierarchy (shared object though, so future merging is not a problem).
 * 
 * So it is a bit like a disjoint set forest, but I can't find or think of a way to allow for splitting in the graph efficiently... ):
 * It seems to be the best I can do...
 * 
 * PROBLEM:
 * 
 * I've been efficiently trying to avoid changing the representatives whenever possible... This is a problem, because it makes it hard to know
 * when a representative has been removed (i.e: it hasn't...)
 * Still, if a representative has changed, I should... I think be able to tell... If I just ask the node, potentially asynchronously,
 * what its representative is, then if it didn't change, I should have access right?
 * 
 */
CJO
===
Communicating Java Objects

An attempt at implementing CSO purely in Java!
CSO = Communicating Scala Objects.
Based on CSP - Communicating Sequential Processes first described by C. A. R. Hoare - see
http://en.wikipedia.org/wiki/Communicating_sequential_processes (link not maintained)

CSO provides an abstraction for concurrent programming far above semaphores and monitors. It makes use of "channels" or "pipes" down which processes send messages to communicate with. They are forced to synchronise on these pipes!
Channels typically make the purpose of parallel programs considerably clearer, and are usually much easier to reason about. It may even be the case in the future that you can just pump a CSO program into a model checker to prove correctness automatically! (Via FDR?)

To give a brief idea of how useful the abstraction is, consider the following example written in the CSO language:

def tagger[T](left : ?[T], right: ?[T], out: ![(int,T)]) = proc {
  repeat {
    alt(
      left -?-> {out!(0, left?)}
      right -?-> {out!(1, right?)}
    )
  }
  left.closein;right.closein;out.closeout;
}

The above is a tagger, labelling anything coming from the channel "right" with a one, and from the channel "left" with a zero. Here's how it works:

You read a message from a channel with a "?", so left? reads a message from the left channel (holding messages of type T)
When you read you must synchronise with a writer, so you may be forced to wait.
You write a message to a channel with a "!", so out!(m) writes message m into the out channel.
When you write, you must synchronise with a reader, so you may be forced to wait again.

("repeat" is just a while loop essentially)
The "alt" stands for "alternation", and will generally wait for any number of channels to become "ready" (i.e: the required writer or reader for an interaction has arrived). In this case, the alt waits for one of the left or the right channels to become ready to read from, and it guarantees that only one of its branches will execute at a time.

A slightly more complicated example, showing alts with "guards" which are truth conditions evaluated when the alt
begins waiting to control which branches of the alt are actually able to execute. The example is a part of bag
of tasks with replacement:

repeat {
  alt(
    (!stack.isEmpty &&&  get) -!-> {get!(stack.pop);busyWorkers+=1;}
    (busyWorkers>0  &&&  put) -?-> {val ps=put?;stack.push(ps);}
    (busyWorkers>0  &&& done) -?-> {done?;busyWorkers-=1;}
  )
}

The stack contains all of the tasks, and workers report when they have completed the task via the done channel.
Workers may add new tasks using the put channel.

As you can hopefully see, the meaning of the algorithm is very clear, it is well structured and easy to verify that it is performing the intended task.

CJO is an attempt and a pet project to implement CSO in Java. CSO is very usable, although there are some implementation restrictions in unusual situations, so part of this project's aim is to implement channels to their full potential!
Since it is in Java, it's unlikely we'll be able to make the syntax as pretty, but hopefully it will be very functional. This method of concurrent programming seems better suited to object oriented programming because it is typically easier to separate out concurrency across objects with channels than with other mechanisms.

If you want to collaborate on this, please email
michael.bradley@hotmail.co.uk

Implementation details:

This is a Maven project that I store in a Mecurial Repo (pushing with hg-git). To set it up, make sure you have maven installed, and then you can use the Maven plugin for Eclipse fairly easily. I'm working with Maven 2.2.1 (I think~)

Note:
Softwire is not involved in this project - it was the group id I used when setting up the maven project - I got into the habit of using web addresses. Sorry!!

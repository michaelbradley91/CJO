package com.softwire.it.cjo.operators;

import com.softwire.it.cjo.channels.exceptions.ChannelClosed;
import com.softwire.it.cjo.operators.exceptions.ProcessInterruptedException;

/**
 * ****************<br>
 * Date: 18/03/2014<br>
 * Author:  michael<br>
 * ****************<br>
 * <br>
 * This class provides static access to many operations applied to channels.<br>
 * It is meant to be statically imported to reduce some of the syntax
 *
 */
public class Ops {
	/**
	 * Hide the constructor
	 */
	private Ops() {};
	
	/**
	 * Read a message from a channel. Note that illegal state exceptions may be thrown if the underlying channel is improperly
	 * used.
	 * @param channel - the channel to read from
	 * @return - a message from the channel
	 * @throws ProcessInterruptedException - if the process is interrupted before it receives a message
	 * @throws ChannelClosed - if the channel you were reading from closed before you received a message
	 */
	public static <Message> Message read(Channel<Message> channel) {
		return new Read<Message>().read(channel);
	}
	
	/**
	 * Read a message from a channel. Note that illegal state exceptions may be thrown if the underlying channel is improperly
	 * used.
	 * @param channel - the channel to read from
	 * @return - a message from the channel
	 * @throws ProcessInterruptedException - if the process is interrupted before it receives a message
	 * @throws ChannelClosed - if the channel you were reading from closed before you received a message
	 */
	public static <Message> Message read(ChannelReader<Message> channel) {
		return new Read<Message>().read(channel);
	}
	
	/**
	 * Write a message into a channel. Note that illegal state exceptions may be thrown if the underlying channel is improperly
	 * used.
	 * @param channel - the channel to write into
	 * @param message - the message to write
	 * @return - a message from the channel
	 * @throws ProcessInterruptedException - if the process is interrupted before it can send a message
	 * @throws ChannelClosed - if the channel you were writing to closed before you sent a message
	 */
	public static <Message> void write(Channel<Message> channel, Message message) {
		new Write<Message>().write(channel,message);
	}
	
	/**
	 * Write a message into a channel. Note that illegal state exceptions may be thrown if the underlying channel is improperly
	 * used.
	 * @param channel - the channel to write into
	 * @param message - the message to write
	 * @return - a message from the channel
	 * @throws ProcessInterruptedException - if the process is interrupted before it can send a message
	 * @throws ChannelClosed - if the channel you were writing to closed before you sent a message
	 */
	public static <Message> void write(ChannelWriter<Message> channel, Message message) {
		new Write<Message>().write(channel,message);
	}
	
	/**
	 * Close the read end of a channel. This does nothing if that channel is already closed.
	 * @param channel - the one to close
	 */
	public static <Message> void closeReadEnd(ChannelReader<Message> channel) {
		new Close<Message>().closeReadEnd(channel);
	}
	
	/**
	 * Close the read end of a channel. This does nothing if that channel is already closed.
	 * @param channel - the one to close
	 */
	public static <Message> void closeReadEnd(Channel<Message> channel) {
		new Close<Message>().closeReadEnd(channel);
	}
	
	/**
	 * Close the write end of a channel. This does nothing if that channel is already closed.
	 * @param channel - the one to close
	 */
	public static <Message> void closeWriteEnd(ChannelWriter<Message> channel) {
		new Close<Message>().closeWriteEnd(channel);
	}
	
	/**
	 * Close the write end of a channel. This does nothing if that channel is already closed.
	 * @param channel - the one to close
	 */
	public static <Message> void closeWriteEnd(Channel<Message> channel) {
		new Close<Message>().closeWriteEnd(channel);
	}
	
	/**
	 * Close the channel. This does nothing if that channel is already closed.
	 * @param channel - the one to close
	 */
	public static <Message> void close(Channel<Message> channel) {
		new Close<Message>().close(channel);
	}
	
	/**
	 * Execute an alt derived from an alt builder! Alts are rather complicated. For details, go to AltBuilder
	 * and Alt. This operator will try to implement fairness between branches by random selection. (The guards
	 * and message evaluations still take place in the same order if you were concerned)
	 * @param builder - the builder used to construct this alt
	 * @see Alt
	 * @see AltBuilder
	 */
	public static void alt(AltBuilder builder) {
		//Construct the alt
		Alt alt = new Alt(builder);
		//Randomly select the branch and run it!
		alt.run(0, true);
	}
	
	/**
	 * Execute an alt derived from an alt builder! Alts are rather complicated. For details, go to AltBuilder
	 * and Alt. This operator will give priority to branches in order of when they were added, with
	 * the LAST branch having the highest priority. Sorry if that's a confusing way round! (The guards
	 * and message evaluations still take place in the same order if you were concerned)
	 * @param builder - the builder used to construct this alt
	 * @see Alt
	 * @see AltBuilder
	 */
	public static void priAlt(AltBuilder builder) {
		//Construct the alt
		Alt alt = new Alt(builder);
		//Always start from the top
		alt.run(0, false);
	}
	
	/**
	 * Execute an alt derived from an alt builder! Alts are rather complicated. For details, go to AltBuilder
	 * and Alt. Serve runs the alt in a while loop (until an exception is thrown). The alt internally
	 * will choose its branch priority randomly for fairness. This is considered more fair than round robin,
	 * since unfortunate relationships between guards and branch activations could cause round robin to ignore a branch. (The guards
	 * and message evaluations still take place in the same order if you were concerned)
	 * @param builder - the builder used to construct this alt
	 * @see Alt
	 * @see AltBuilder
	 */
	public static void serve(AltBuilder builder) {
		while(true) {
			alt(builder);
		}
	}
	
	/**
	 * Execute an alt derived from an alt builder! Alts are rather complicated. For details, go to AltBuilder
	 * and Alt. PriServe runs the alt in a while loop (until an exception is thrown). The alt internally
	 * will give priority to the branches in the order they were added, with the LAST branch having highest priority. (The guards
	 * and message evaluations still take place in the same order if you were concerned)
	 * @param builder - the builder used to construct this alt
	 * @see Alt
	 * @see AltBuilder
	 */
	public static void priServe(AltBuilder builder) {
		while(true) {
			priAlt(builder);
		}
	}
}

package com.redhat.training.jb421;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DelayBean {
	
	@Value("${replyDelay}")
	private Integer replyDelay;
	
	public void waitDelay() throws InterruptedException {
		Thread.sleep(replyDelay);
	}

}

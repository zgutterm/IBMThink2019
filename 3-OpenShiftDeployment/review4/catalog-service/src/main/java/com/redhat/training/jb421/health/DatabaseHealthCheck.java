package com.redhat.training.jb421.health;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class DatabaseHealthCheck implements HealthIndicator {
	
	@Autowired
	private EntityManagerFactory entityManagerFactory;

	public Health health() {
		
		
		try {
			EntityManager entityManager = entityManagerFactory.createEntityManager();
			Query q = entityManager.createNativeQuery("select 1");
			q.getFirstResult();
			return Health.up().build();
		}catch(Exception e) {
			return Health.down(e).build();
		}
		
		
	}

}

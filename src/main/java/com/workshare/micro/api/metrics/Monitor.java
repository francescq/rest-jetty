package com.workshare.micro.api.metrics;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;

public class Monitor {
    
    private final MetricRegistry metrics = new MetricRegistry();
    private final HealthCheckRegistry healthChecks = new HealthCheckRegistry();

    public MetricRegistry metrics() {
        return metrics;
    }

    public HealthCheckRegistry checks() {
        return healthChecks;
    }
}

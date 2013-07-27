package com.workshare.servlet;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.google.inject.Injector;
import com.workshare.micro.api.metrics.Monitor;

public class MetricsFilter implements javax.servlet.Filter {
    
    private static final Logger logger = LoggerFactory.getLogger(MetricsFilter.class);

    private Meter requests;
    private Timer responses;
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        
        requests.mark();

        final long now = System.nanoTime();
        final Timer.Context context = responses.time();
        try {
            chain.doFilter(request, response);
        }
        finally {
            context.stop();

            if (logger.isDebugEnabled()) {
                final long elapsed = System.nanoTime() - now;
                final HttpServletRequest httpRequest = (HttpServletRequest)request;
                final long micros = (long)(elapsed/1000.0);
                logger.debug("Request {} server in {} millis", httpRequest.getMethod()+":"+httpRequest.getPathInfo(), micros/1000.0);
            }
        }
    }

    @Override
    public void init(FilterConfig config) throws ServletException {
        Injector injector = (Injector)config.getServletContext().getAttribute(Injector.class.getName());
        Monitor monitor = injector.getInstance(Monitor.class);
        requests = monitor.metrics().meter("http.requests");
        responses = monitor.metrics().timer("http.responses");
    }

    @Override
    public void destroy() {
    }

}

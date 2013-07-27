package com.workshare.micro.utils;

import com.fasterxml.uuid.EthernetAddress;
import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedGenerator;

public class UUIDGenerator {
    
    static TimeBasedGenerator generator;
    static {
        generator = Generators.timeBasedGenerator(EthernetAddress.fromInterface());
    }
    
    public String generateString() {
	return generator.generate().toString();
    }
}

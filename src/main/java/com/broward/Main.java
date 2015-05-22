package com.broward;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;

public class Main {

	public static void main(String[] args) {
		
		for (String arg: args) {
			System.out.println("arg=" + arg);
		}
		
		EmbeddedZookeeper embeddedZookeeper = new EmbeddedZookeeper(2181);
		List<Integer> kafkaPorts = new ArrayList<Integer>();
		// -1 for any available port
		kafkaPorts.add(-1);

		try {
			int brokerid = 0;
			if (args != null) {
				if ("brokerid=0".equals(args[0])) {
					embeddedZookeeper.startup();
				}
				int index = args[0].indexOf("=");
				brokerid = Integer.parseInt(args[0].substring(index+1, args[0].length()));
			}
			
			System.out.println("brokerid is " + brokerid);
			EmbeddedKafkaCluster embeddedKafkaCluster = new EmbeddedKafkaCluster(
					embeddedZookeeper.getConnection(), new Properties(),
					kafkaPorts, brokerid);
			
			
			System.out.println("### Embedded Zookeeper connection: "
					+ embeddedZookeeper.getConnection());
			embeddedKafkaCluster.startup();
			System.out.println("### Embedded Kafka cluster broker list: "
					+ embeddedKafkaCluster.getBrokerList());
			
			// enable JMX port
			loadJMXAgent(9996 + brokerid);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Thread.sleep(10000);
		// / embeddedKafkaCluster.shutdown();
		// embeddedZookeeper.shutdown();
	}

	public static String loadJMXAgent(int port) throws IOException, AttachNotSupportedException, AgentLoadException, AgentInitializationException {
		  String name = ManagementFactory.getRuntimeMXBean().getName();
		    VirtualMachine vm = VirtualMachine.attach(name.substring(0,
		            name.indexOf('@')));

		    String lca = vm.getAgentProperties().getProperty(
		            "com.sun.management.jmxremote.localConnectorAddress");
		    if (lca == null) {
		    	System.out.println("lca was null");
		        Path p = Paths.get(System.getProperty("java.home")).normalize();
		        if (!"jre".equals(p.getName(p.getNameCount() - 1).toString()
		                .toLowerCase())) {
		            p = p.resolve("jre");
		        }
		        File f = p.resolve("lib").resolve("management-agent.jar").toFile();
		        if (!f.exists()) {
		            throw new IOException("Management agent not found");
		        }
		        String options = String.format("com.sun.management.jmxremote.port=%d, " +
		                "com.sun.management.jmxremote.authenticate=false, " +
		                "com.sun.management.jmxremote.ssl=false", port);
		        vm.loadAgent(f.getCanonicalPath(), options);
		        lca = vm.getAgentProperties().getProperty(
		                "com.sun.management.jmxremote.localConnectorAddress");
		    }
		    vm.detach();
		    return lca;
	}
}

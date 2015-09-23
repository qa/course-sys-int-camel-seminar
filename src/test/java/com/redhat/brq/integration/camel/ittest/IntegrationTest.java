package com.redhat.brq.integration.camel.ittest;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.KarafDistributionBaseConfigurationOption;
import org.ops4j.pax.exam.karaf.options.KarafDistributionKitConfigurationOption;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.util.Filter;
import org.osgi.framework.BundleContext;

import com.redhat.brq.integration.camel.model.Address;
import com.redhat.brq.integration.camel.model.Order;
import com.redhat.brq.integration.camel.model.OrderItem;
import com.redhat.brq.integration.camel.service.OrderRepository;

import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFileExtend;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.replaceConfigurationFile;

// TASK-10
// Finish integration test.
// Add @RunWith PaxExam class to run test inside JBoss Fuse with PaxExam
@RunWith(PaxExam.class) // Run test inside jboss-fuse with PaxExam
@ExamReactorStrategy(PerClass.class) // Alternatively PerMethod (Fuse started / stopped for each test method)
public class IntegrationTest extends CamelTestSupport {

    public static final String FUSE_PATH = System.getProperty("fuse.zip.url", null);
    public static final String ENDPOINT_FILE_BASEURL = System.getProperty("endpoint.file.baseUrl", null);

    public static final String KARAF_VERSION = "2.4.0"; // Karaf version in JBoss Fuse 6.2

    /**
     * Configure pax exam test framework. <br/>
     * <p/>
     * Check PaxExam documentation on <a href="https://ops4j1.jira.com/wiki/display/PAXEXAM3/Karaf+Test+Container+Reference#KarafTestContainerReference-replaceConfigurationFile">
     * OPS4J Wiki</a>.
     *
     * @return PaxExam configuration
     */
    @Configuration
    public Option[] configure() {

        if(ENDPOINT_FILE_BASEURL == null) {
            throw new IllegalArgumentException(String.format("Property '%s' must be defined.", "FILE"));
        }

        // Configure startup of JBoss Fuse
        final KarafDistributionBaseConfigurationOption baseConfig = karafDistributionConfiguration()
            // Path to zip with JBoss Fuse 6.2
            .frameworkUrl(FUSE_PATH)
            // Tell PaxExam which Karaf version is under test
            .karafVersion(KARAF_VERSION)
            // Tell PaxExam, where to unpack JBoss fuse. Further in the comments it's referred as $FUSE_HOME
            .unpackDirectory(new File("target/unpacked-fuse"))
            // Deploy bundles through feature, instead of copying to $FUSE_HOME/deploy
            // You can check feature descriptor in target/unpacked-fuse/{some hash}/test-dependencies.xml
            .useDeployFolder(false);

        return new Option[] {
            // Replace default ActiveMQ configuration with config from test resources
            replaceConfigurationFile("etc/activemq.xml", new File("src/test/resources/activemq.xml")),
            // Copy keystore.jks from src/main/resources to $FUSE_HOME/bin/keystore.jks
            replaceConfigurationFile("bin/keystore.jks", new File("src/main/resources/keystore.jks")),

            // Automatically install required features during fuse startup
            editConfigurationFileExtend("etc/org.apache.karaf.features.cfg",
                "featuresBoot", ", camel-http4, camel-jackson, camel-restlet, camel-test"),

            // This is kind of hack to propagate java property into Fuse.
            editConfigurationFilePut("etc/system.properties", "endpoint.file.baseUrl", ENDPOINT_FILE_BASEURL),

            // Add user admin with password admin and all default roles
            editConfigurationFilePut("etc/users.properties", "admin",
                "admin,admin,manager,viewer,Monitor, Operator, Maintainer, Deployer, Auditor, Administrator, SuperUser"),
            // Add user shipuser with password shippwd and role admin
            editConfigurationFilePut("etc/users.properties", "shipuser", "shippwd,admin"),

            // Don't delete $FUSE_HOME after test ends
            keepRuntimeFolder(),

            // First execute mvn clean install -DskipTests to install this bundle into local maven repo.
            // When tests are executed, PaxExam will deploy this bundle from local maven repo.
            // Alternatively, you could use streamBundle() to create bundle during the test execution,
            // but it requires listing all classes, that should be included, and specifying proper osgi headers.
            bundle("mvn:com.redhat.brq.integration/camel-seminar/1.0.0-SNAPSHOT"),

            // Force PaxExam to use startup scripts (bin/fuse).
            // Pax will ignore configuration, that doesn't match current platform,
            // so it is safe to add config for both windows and unix.
            // Both windows and unix configurations have the same framework url,
            // defined few lines earlier in baseConfig variable.
            new KarafDistributionKitConfigurationOption(baseConfig,
                KarafDistributionKitConfigurationOption.Platform.NIX).executable("bin/fuse"),

            new KarafDistributionKitConfigurationOption(baseConfig,
                KarafDistributionKitConfigurationOption.Platform.WINDOWS).executable("bin\\fuse.bat")

            // It is possible to use just baseConfig, without specifying executable script,
            // but PaxExam would start fuse with Java runner, and it would lack configuration from bin/fuse script.
        };
    }


    @Inject
    // Filter in case there is more Camel contexts deployed
    // Make sure, that name in this filter matches id="camel" in spring/blueprint camelContext tag
    @Filter("(camel.context.name=camel)")
    CamelContext context;

    @Inject
    BundleContext bundleContext; // OSGI bundle context

    @EndpointInject(uri = "direct:new-order")
    private ProducerTemplate producer;

    @EndpointInject(uri = "mock:new-order-resp")
    private MockEndpoint response;

    @Override
    protected void doPreSetup() throws Exception {
        assertNotNull("Camel context should exist", context);
        assertNotNull("Bundle context should exist", bundleContext);
    }

    @Override
    protected CamelContext createCamelContext() throws Exception {
        return context;
    }

    @Override
    public boolean isCreateCamelContextPerClass() {
        // we override this method and return true, to tell Camel test-kit that it should only create CamelContext
        // once (per class), so we will re-use the CamelContext between each test method in this class
        return true;
    }

    @Before
    public void before() {
        // @Before methods are invoked inside container with access to services like bundle context
        System.out.println("\n@Before\n");
        System.out.println("BundleContext: " + bundleContext);
    }

    @Test
    public void itTest() throws Exception {
        context.getRouteDefinition("new-order").adviceWith((ModelCamelContext) context, new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                interceptSendToEndpoint("seda:issue-order")
                    .to("mock:new-order-resp");
            }
        });

        response.expectedMessageCount(1);
        // TASK-10: HTTP response code 201 is expected
        // TASK-10: Location header is expected to be '/orders/1'
        response.expectedHeaderReceived(Exchange.HTTP_RESPONSE_CODE, 201);
        response.expectedHeaderReceived("Location", "/orders/1");

        // TASK-10: send body - use createOrder() method to create body
        producer.sendBody(createOrder());

        Thread.sleep(TimeUnit.SECONDS.toMillis(20)); // wait for process

        response.assertIsSatisfied();
        assertNotNull(OrderRepository.get(1L));
        assertEquals("CONFIRMED", OrderRepository.get(1L).getStatus().getResolution());
    }

    private Order createOrder() {
        List<OrderItem> items = new ArrayList<>(2);
        items.add(new OrderItem(9L, 2, 2.0));
        items.add(new OrderItem(8L, 2, 2.0));
        return new Order(1L, items, new Address("Jan", "Novak", "Purkynova", "Brno", "61200"), null);
    }
}

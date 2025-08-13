package com.healthassistant.actors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.cluster.typed.Cluster;
import akka.cluster.typed.ClusterSingleton;
import akka.cluster.typed.SingletonActor;
import com.healthassistant.messages.RouterMessages.*;
import com.healthassistant.utils.DepartmentClassifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * RouterActor routes user queries to appropriate department actors based on query content.
 * Demonstrates 'tell' pattern for routing messages.
 */
public class RouterActor extends AbstractBehavior<UserQuery> {
    private static final Logger logger = LoggerFactory.getLogger(RouterActor.class);

    private final Map<String, ActorRef<Object>> departmentActors;
    private final String nodeName;

    public static Behavior<UserQuery> create() {
        return Behaviors.setup(RouterActor::new);
    }

    private RouterActor(ActorContext<UserQuery> context) {
        super(context);
        this.departmentActors = new HashMap<>();
        this.nodeName = Cluster.get(context.getSystem()).selfMember().address().toString();
        
        logger.info("RouterActor started on node: {}", nodeName);
        
        // Initialize department actors - in a real cluster, these would be distributed
        initializeDepartmentActors();
    }

    private void initializeDepartmentActors() {
        // Create or get references to department actors
        ActorRef<Object> generalMedicineActor = getContext().spawn(
            GeneralMedicineActor.create(), "general-medicine-actor");
        ActorRef<Object> pharmacyActor = getContext().spawn(
            PharmacyActor.create(), "pharmacy-actor");
        ActorRef<Object> radiologyActor = getContext().spawn(
            RadiologyActor.create(), "radiology-actor");

        departmentActors.put("general-medicine", generalMedicineActor);
        departmentActors.put("pharmacy", pharmacyActor);
        departmentActors.put("radiology", radiologyActor);

        logger.info("Initialized {} department actors", departmentActors.size());
    }

    @Override
    public Receive<UserQuery> createReceive() {
        return newReceiveBuilder()
                .onMessage(UserQuery.class, this::routeUserQuery)
                .build();
    }

    private Behavior<UserQuery> routeUserQuery(UserQuery query) {
        String queryId = UUID.randomUUID().toString();
        String department = determineDepartment(query.query);
        
        logger.info("Routing query {} from user {} to department: {} (Query: '{}')", 
                queryId, query.userId, department, query.query);

        ActorRef<Object> departmentActor = departmentActors.get(department);
        
        if (departmentActor != null) {
            // Using 'tell' pattern - fire and forget
            RouteQuery routeQuery = new RouteQuery(query.query, query.userId, queryId, query.replyTo);
            departmentActor.tell(routeQuery);
            
            logger.info("Successfully routed query {} to {} department using 'tell' pattern", 
                    queryId, department);
        } else {
            logger.warn("No actor found for department: {}, defaulting to general medicine", department);
            
            // Fallback to general medicine
            ActorRef<Object> defaultActor = departmentActors.get("general-medicine");
            if (defaultActor != null) {
                RouteQuery routeQuery = new RouteQuery(query.query, query.userId, queryId, query.replyTo);
                defaultActor.tell(routeQuery);
            } else {
                // Send error response
                QueryResponse errorResponse = new QueryResponse(
                        "Service temporarily unavailable. Please try again later.",
                        "error",
                        queryId,
                        false
                );
                query.replyTo.tell(errorResponse);
            }
        }

        return this;
    }

    /**
     * Determine department based on query content using the classifier utility
     */
    private String determineDepartment(String query) {
        return DepartmentClassifier.classifyQuery(query);
    }

    /**
     * Create a cluster singleton router actor
     */
    public static ActorRef<UserQuery> createSingleton(ActorContext<?> context) {
        ClusterSingleton singleton = ClusterSingleton.get(context.getSystem());
        
        return singleton.init(SingletonActor.of(
                RouterActor.create(),
                "router-singleton"
        ));
    }
}

package com.saasplatform.conversation.resource;

import com.saasplatform.common.dto.ApiResponse;
import com.saasplatform.conversation.domain.Conversation;
import com.saasplatform.conversation.domain.Message;
import com.saasplatform.conversation.service.ConversationService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

@Path("/api/v1/conversations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"OWNER", "ADMIN", "EMPLOYEE"})
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Conversations", description = "Customer conversation management and human handoff")
public class ConversationResource {

    @Inject
    ConversationService conversationService;

    @GET
    public Response list() {
        List<Conversation> convs = conversationService.listConversations();
        return Response.ok(ApiResponse.ok(convs)).build();
    }

    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") UUID id) {
        return Response.ok(ApiResponse.ok(conversationService.getConversation(id))).build();
    }

    @GET
    @Path("/{id}/messages")
    public Response getMessages(@PathParam("id") UUID id) {
        List<Message> messages = conversationService.getMessages(id);
        return Response.ok(ApiResponse.ok(messages)).build();
    }

    @POST
    @Path("/{id}/messages")
    public Response sendReply(@PathParam("id") UUID id, ReplyRequest request) {
        Message msg = conversationService.sendHumanReply(id, request.content());
        return Response.ok(ApiResponse.ok("Message sent", msg)).build();
    }

    @POST
    @Path("/{id}/take-over")
    @RolesAllowed({"OWNER", "ADMIN"})
    public Response takeOver(@PathParam("id") UUID id) {
        Conversation conv = conversationService.takeOver(id);
        return Response.ok(ApiResponse.ok("Conversation taken over", conv)).build();
    }

    @POST
    @Path("/{id}/release")
    @RolesAllowed({"OWNER", "ADMIN"})
    public Response release(@PathParam("id") UUID id) {
        Conversation conv = conversationService.releaseToAi(id);
        return Response.ok(ApiResponse.ok("Conversation released to AI", conv)).build();
    }

    @POST
    @Path("/{id}/resolve")
    @RolesAllowed({"OWNER", "ADMIN"})
    public Response resolve(@PathParam("id") UUID id) {
        Conversation conv = conversationService.resolve(id);
        return Response.ok(ApiResponse.ok("Conversation resolved", conv)).build();
    }

    public record ReplyRequest(String content) {}
}

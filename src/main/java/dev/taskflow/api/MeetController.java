package dev.taskflow.api;

import dev.taskflow.api.Dtos.*;
import dev.taskflow.security.CurrentUser;
import io.livekit.server.AccessToken;
import io.livekit.server.RoomJoin;
import io.livekit.server.RoomName;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/meet")
public class MeetController {

    @Value("${livekit.url}") private String lkUrl;
    @Value("${livekit.api-key}") private String lkKey;
    @Value("${livekit.api-secret}") private String lkSecret;

    @PostMapping("/start")
    public StartMeetRes start(@RequestBody(required = false) StartMeetReq req) {
        CurrentUser.require();
        String roomId = "tf-" + UUID.randomUUID().toString().substring(0, 12);
        String url = "/meet/" + roomId;
        return new StartMeetRes(roomId, url);
    }

    @GetMapping("/{roomId}/token")
    public MeetTokenRes token(@PathVariable String roomId) {
        var me = CurrentUser.require();
        if (lkUrl == null || lkUrl.isBlank() || lkKey == null || lkKey.isBlank() || lkSecret == null || lkSecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "LiveKit is not configured (LIVEKIT_URL / LIVEKIT_API_KEY / LIVEKIT_API_SECRET).");
        }
        AccessToken token = new AccessToken(lkKey, lkSecret);
        token.setIdentity(me.id().toString());
        token.setName(me.username());
        token.addGrants(new RoomJoin(true), new RoomName(roomId));
        return new MeetTokenRes(token.toJwt(), lkUrl);
    }
}

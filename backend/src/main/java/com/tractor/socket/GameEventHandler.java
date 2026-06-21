package com.tractor.socket;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.OnConnect;
import com.corundumstudio.socketio.annotation.OnDisconnect;
import com.corundumstudio.socketio.annotation.OnEvent;
import com.tractor.game.GameRoom;
import com.tractor.model.Player;
import com.tractor.model.Suit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class GameEventHandler {

    private final SocketIOServer server;
    private final GameRoom gameRoom = GameRoom.getInstance();

    @Autowired
    public GameEventHandler(SocketIOServer server) {
        this.server = server;
    }

    @OnConnect
    public void onConnect(SocketIOClient client) {
        System.out.println("客户端连接: " + client.getSessionId());
    }

    @OnDisconnect
    public void onDisconnect(SocketIOClient client) {
        System.out.println("客户端断开: " + client.getSessionId());
        String playerId = getPlayerId(client);
        if (playerId != null) {
            Player player = gameRoom.getPlayerById(playerId);
            if (player != null) {
                String nickname = player.getNickname();
                gameRoom.removePlayer(playerId);
                broadcastGameState();
                server.getBroadcastOperations().sendEvent("playerLeft", nickname);
            }
        }
    }

    @OnEvent("joinRoom")
    public void onJoinRoom(SocketIOClient client, Map<String, String> data, AckRequest ackRequest) {
        String nickname = data.get("nickname");
        if (nickname == null || nickname.trim().isEmpty()) {
            if (ackRequest.isAckRequested()) {
                ackRequest.sendAckData(false, "昵称不能为空");
            }
            return;
        }
        if (gameRoom.getPlayerCount() >= 4) {
            if (ackRequest.isAckRequested()) {
                ackRequest.sendAckData(false, "房间已满");
            }
            return;
        }
        String playerId = UUID.randomUUID().toString();
        client.set("playerId", playerId);
        boolean success = gameRoom.addPlayer(playerId, nickname.trim(), client);
        if (success) {
            if (ackRequest.isAckRequested()) {
                ackRequest.sendAckData(true, playerId);
            }
            broadcastGameState();
            server.getBroadcastOperations().sendEvent("playerJoined", nickname.trim());
        } else {
            if (ackRequest.isAckRequested()) {
                ackRequest.sendAckData(false, "加入失败");
            }
        }
    }

    @OnEvent("declareTrump")
    public void onDeclareTrump(SocketIOClient client, Map<String, String> data, AckRequest ackRequest) {
        String playerId = getPlayerId(client);
        if (playerId == null) {
            sendErrorAck(ackRequest, "请先加入房间");
            return;
        }
        String suitStr = data.get("suit");
        Suit suit;
        try {
            suit = Suit.valueOf(suitStr);
        } catch (Exception e) {
            sendErrorAck(ackRequest, "花色无效");
            return;
        }
        boolean success = gameRoom.declareTrump(playerId, suit);
        if (success) {
            if (ackRequest.isAckRequested()) {
                ackRequest.sendAckData(true, "亮主成功");
            }
            Player player = gameRoom.getPlayerById(playerId);
            server.getBroadcastOperations().sendEvent("trumpDeclared",
                player.getNickname(), suit.getSymbol());
            broadcastGameState();
        } else {
            sendErrorAck(ackRequest, "亮主失败");
        }
    }

    @OnEvent("skipDeclare")
    public void onSkipDeclare(SocketIOClient client, AckRequest ackRequest) {
        String playerId = getPlayerId(client);
        if (playerId == null) {
            sendErrorAck(ackRequest, "请先加入房间");
            return;
        }
        boolean success = gameRoom.skipDeclare(playerId);
        if (success) {
            if (ackRequest.isAckRequested()) {
                ackRequest.sendAckData(true);
            }
            broadcastGameState();
        } else {
            sendErrorAck(ackRequest, "操作失败");
        }
    }

    @OnEvent("discardBottom")
    public void onDiscardBottom(SocketIOClient client, Map<String, Object> data, AckRequest ackRequest) {
        String playerId = getPlayerId(client);
        if (playerId == null) {
            sendErrorAck(ackRequest, "请先加入房间");
            return;
        }
        @SuppressWarnings("unchecked")
        List<Integer> cardIds = (List<Integer>) data.get("cardIds");
        boolean success = gameRoom.discardBottomCards(playerId, cardIds);
        if (success) {
            if (ackRequest.isAckRequested()) {
                ackRequest.sendAckData(true, "扣底成功");
            }
            Player player = gameRoom.getPlayerById(playerId);
            server.getBroadcastOperations().sendEvent("bottomDiscarded", player.getNickname());
            broadcastGameState();
        } else {
            sendErrorAck(ackRequest, "扣底失败");
        }
    }

    @OnEvent("playCards")
    public void onPlayCards(SocketIOClient client, Map<String, Object> data, AckRequest ackRequest) {
        String playerId = getPlayerId(client);
        if (playerId == null) {
            sendErrorAck(ackRequest, "请先加入房间");
            return;
        }
        @SuppressWarnings("unchecked")
        List<Integer> cardIds = (List<Integer>) data.get("cardIds");
        boolean success = gameRoom.playCards(playerId, cardIds);
        if (success) {
            if (ackRequest.isAckRequested()) {
                ackRequest.sendAckData(true, "出牌成功");
            }
            broadcastGameState();
            Player player = gameRoom.getPlayerById(playerId);
            server.getBroadcastOperations().sendEvent("cardsPlayed", player.getNickname(), cardIds.size());
        } else {
            sendErrorAck(ackRequest, "出牌失败");
        }
    }

    @OnEvent("getGameState")
    public void onGetGameState(SocketIOClient client, AckRequest ackRequest) {
        String playerId = getPlayerId(client);
        if (playerId != null) {
            Map<String, Object> state = gameRoom.getPlayerState(playerId);
            if (ackRequest.isAckRequested()) {
                ackRequest.sendAckData(state);
            }
        }
    }

    @OnEvent("leaveRoom")
    public void onLeaveRoom(SocketIOClient client, AckRequest ackRequest) {
        String playerId = getPlayerId(client);
        if (playerId != null) {
            Player player = gameRoom.getPlayerById(playerId);
            String nickname = player != null ? player.getNickname() : "";
            gameRoom.removePlayer(playerId);
            if (ackRequest.isAckRequested()) {
                ackRequest.sendAckData(true);
            }
            broadcastGameState();
            if (!nickname.isEmpty()) {
                server.getBroadcastOperations().sendEvent("playerLeft", nickname);
            }
        }
    }

    private String getPlayerId(SocketIOClient client) {
        return client.get("playerId");
    }

    private void sendErrorAck(AckRequest ackRequest, String message) {
        if (ackRequest.isAckRequested()) {
            ackRequest.sendAckData(false, message);
        }
    }

    private void broadcastGameState() {
        for (Player player : gameRoom.getPlayers()) {
            if (player.getClient() != null) {
                Map<String, Object> state = gameRoom.getPlayerState(player.getId());
                player.getClient().sendEvent("gameState", state);
            }
        }
    }
}

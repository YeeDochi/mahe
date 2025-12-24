package org.example.mahe.dto;

import java.util.*;

public class MaheRoom extends BaseGameRoom {

    private static final int TRACK_LENGTH = 21;
    private static final int RAFT_POSITION = -1;

    private List<Integer> eggDeck = new ArrayList<>();
    private int currentEggCard = 0;
    private boolean lastCardTaken = false;

    private List<String> turnOrder = new ArrayList<>();
    private int currentTurnIdx = 0;
    private Map<String, Integer> turtlePositions = new HashMap<>();
    private Map<String, List<Integer>> playerEggCards = new HashMap<>();
    private Map<Integer, List<String>> positionStacks = new HashMap<>();

    private List<Integer> currentRolls = new ArrayList<>();
    private int diceCount = 0;
    private boolean mustRoll = true;
    private String actualTurnPlayer = "";

    public MaheRoom(String name) {
        super(name);
    }

    public void startGame() {
        if (users.size() < 2) return;

        eggDeck.clear();
        for (int i = 1; i <= 6; i++) {
            eggDeck.add(i);
            eggDeck.add(i);
        }
        eggDeck.add(7);
        Collections.shuffle(eggDeck);

        currentEggCard = eggDeck.remove(0);
        lastCardTaken = false;

        turnOrder = new ArrayList<>(users.keySet());
        Collections.shuffle(turnOrder);
        currentTurnIdx = 0;

        turtlePositions.clear();
        playerEggCards.clear();
        positionStacks.clear();

        positionStacks.put(RAFT_POSITION, new ArrayList<>());
        for (String pid : turnOrder) {
            turtlePositions.put(pid, RAFT_POSITION);
            playerEggCards.put(pid, new ArrayList<>());
            positionStacks.get(RAFT_POSITION).add(pid);
        }

        resetDice();
        updateActualTurnPlayer();
        this.playing = true;
    }

    private void resetDice() {
        currentRolls.clear();
        diceCount = 0;
        mustRoll = true;
    }

    private void updateActualTurnPlayer() {
        String basePlayer = turnOrder.get(currentTurnIdx);
        Integer pos = turtlePositions.get(basePlayer);
        if (pos == null) {
            actualTurnPlayer = basePlayer;
            return;
        }

        if (pos == RAFT_POSITION) {
            actualTurnPlayer = basePlayer;
            return;
        }
        List<String> stack = positionStacks.getOrDefault(pos, new ArrayList<>());
        if (stack.isEmpty() || !stack.contains(basePlayer)) {
            actualTurnPlayer = basePlayer;
        } else {
            actualTurnPlayer = stack.get(stack.size() - 1);
        }
    }

    @Override
    public synchronized GameMessage handleAction(GameMessage message) {
        String type = (String) message.getData().get("actionType");
        String senderId = message.getSenderId();

        if ("START".equals(type)) {
            if (users.size() < 2) return makeChat("SYSTEM", "최소 2명이 필요합니다.");
            startGame();
            return makeMessage("UPDATE", "🐢 마헤 게임 시작! 첫 턴: " + getNickname(actualTurnPlayer) + " | 알 카드: " + currentEggCard + "점");
        }

        if (!playing) return null;
        if (!senderId.equals(actualTurnPlayer)) return null;

        if ("ROLL".equals(type)) return handleRoll(senderId);
        if ("STOP".equals(type)) return handleStop(senderId);

        return null;
    }

    private GameMessage handleRoll(String playerId) {
        if (diceCount >= 3) return makeChat("SYSTEM", "주사위는 최대 3번까지입니다.");

        int currentSum = currentRolls.stream().mapToInt(Integer::intValue).sum();
        if (currentSum >= 7 && diceCount > 0) return makeChat("SYSTEM", "합이 7입니다. 멈추기를 선택하세요.");

        int dice = new Random().nextInt(6) + 1;
        currentRolls.add(dice);
        diceCount++;
        mustRoll = false;

        int newSum = currentRolls.stream().mapToInt(Integer::intValue).sum();
        String playerName = getNickname(playerId);
        int previewMove = newSum * diceCount;

        // 버스트 - 주사위 결과도 표시
        if (newSum >= 8) {
            String content = "🎲 " + playerName + ": " + dice + " (합: " + newSum + ") 💥 버스트!";
            handleBurst(playerId);
            return nextTurn(content);
        }

        String basePlayerId = turnOrder.get(currentTurnIdx);
        String basePlayerName = getNickname(basePlayerId);
        String controllerName = getNickname(playerId);

        String nameDisplay = basePlayerName;
        if (!basePlayerId.equals(playerId)) {
            nameDisplay += "(" + controllerName + " 조종)";
        }

        String content = "🎲 " + nameDisplay + ": " + dice + " (합: " + newSum + ", " + diceCount + "개) → " + previewMove + "칸";

        if (diceCount >= 3 || newSum == 7) {
            return handleStop(playerId);
        }

        return makeMessage("ROLL_RESULT", content);
    }

    private void handleBurst(String playerId) {
        String basePlayer = turnOrder.get(currentTurnIdx);
        int currentPos = turtlePositions.get(basePlayer);

        List<String> stack = positionStacks.getOrDefault(currentPos, new ArrayList<>());
        List<String> toMove = new ArrayList<>();


        if (currentPos == RAFT_POSITION) {
            toMove.add(basePlayer);
            stack.remove(basePlayer);
        } else {
            int playerIdx = stack.indexOf(basePlayer);
            if (playerIdx >= 0) {
                toMove = new ArrayList<>(stack.subList(playerIdx, stack.size()));
                stack.subList(playerIdx, stack.size()).clear();
                if (stack.isEmpty()) positionStacks.remove(currentPos);
            }
        }

        List<String> raftStack = positionStacks.computeIfAbsent(RAFT_POSITION, k -> new ArrayList<>());
        for (String pid : toMove) {
            turtlePositions.put(pid, RAFT_POSITION);
            raftStack.add(pid);
        }

        resetDice();
    }

    private GameMessage handleStop(String playerId) {
        if (mustRoll) return makeChat("SYSTEM", "첫 번째 주사위는 필수입니다.");

        int sum = currentRolls.stream().mapToInt(Integer::intValue).sum();
        int moveAmount = sum * diceCount;

        String basePlayer = turnOrder.get(currentTurnIdx);
        int currentPos = turtlePositions.get(basePlayer);

        List<String> stack = positionStacks.getOrDefault(currentPos, new ArrayList<>());
        List<String> toMove = new ArrayList<>();

        if (currentPos == RAFT_POSITION) {
            toMove.add(basePlayer);
            stack.remove(basePlayer);
        } else {
            int playerIdx = stack.indexOf(basePlayer);
            if (playerIdx >= 0) {
                toMove = new ArrayList<>(stack.subList(playerIdx, stack.size()));
                stack.subList(playerIdx, stack.size()).clear();
                if (stack.isEmpty()) positionStacks.remove(currentPos);
            } else {
                toMove.add(basePlayer);
            }
        }

        int newPos = (currentPos == RAFT_POSITION) ? moveAmount : currentPos + moveAmount;
        String content = "🐢 " + getNickname(basePlayer) + " " + moveAmount + "칸 이동";
        if (toMove.size() > 1) content += " (+" + (toMove.size() - 1) + "마리 업힘)";

        boolean gotEgg = false;
        String eggMessage = "";

        if (newPos >= TRACK_LENGTH) {
            String topTurtle = toMove.get(toMove.size() - 1);
            if (!lastCardTaken) {
                playerEggCards.get(topTurtle).add(currentEggCard);
                eggMessage = getNickname(topTurtle) + "이(가) " + currentEggCard + "점 획득!";
                gotEgg = true;
                if (!eggDeck.isEmpty()) {
                    currentEggCard = eggDeck.remove(0);
                } else {
                    lastCardTaken = true;
                    eggMessage += " (마지막 카드!)";
                }
            } else {
                playerEggCards.get(topTurtle).add(7);
                content += " 🏆 " + getNickname(topTurtle) + " 7점 보너스!";
                resetDice();
                return endGame(content);
            }
            newPos = RAFT_POSITION;
        }

        List<String> newStack = positionStacks.computeIfAbsent(newPos, k -> new ArrayList<>());
        for (String pid : toMove) {
            turtlePositions.put(pid, newPos);
            newStack.add(pid);
        }

        content += " → " + (newPos == RAFT_POSITION ? "뗏목" : newPos + "칸");
        if (gotEgg) content += " 🥚 " + eggMessage;

        resetDice();
        return nextTurn(content);
    }

    private GameMessage nextTurn(String previousContent) {
        currentTurnIdx = (currentTurnIdx + 1) % turnOrder.size();
        updateActualTurnPlayer();

        String basePlayer = turnOrder.get(currentTurnIdx);
        String nextNickname = getNickname(actualTurnPlayer);

        String turnInfo = "\n다음 턴: " + getNickname(basePlayer);

        if (!basePlayer.equals(actualTurnPlayer)) {
            turnInfo += " (조종: " + nextNickname + " 👑)";
        }

        return makeMessage("UPDATE", previousContent + turnInfo);
    }

    private GameMessage endGame(String previousContent) {
        this.playing = false;

        Map<String, Integer> finalScores = new HashMap<>();
        String winnerId = null;
        int maxScore = -1;

        for (String pid : turnOrder) {
            int score = playerEggCards.get(pid).stream().mapToInt(Integer::intValue).sum();
            finalScores.put(pid, score);
            if (score > maxScore) {
                maxScore = score;
                winnerId = pid;
            }
        }

        GameMessage msg = new GameMessage();
        msg.setRoomId(roomId);
        msg.setType("GAME_OVER");
        msg.setContent(previousContent + "\n\n🏆 게임 종료! 승자: " + getNickname(winnerId) + " (" + maxScore + "점)");

        Map<String, Object> data = getGameSnapshot();
        data.put("winnerName", getNickname(winnerId));
        data.put("winnerScore", maxScore);
        data.put("finalScores", finalScores);
        msg.setData(data);

        return msg;
    }

    @Override
    public Map<String, Object> getGameSnapshot() {
        Map<String, String> nicknames = new HashMap<>();
        for (String id : users.keySet()) {
            nicknames.put(id, users.get(id).getNickname());
        }

        Map<String, Integer> scores = new HashMap<>();
        Map<String, Integer> eggCounts = new HashMap<>();
        for (String pid : playerEggCards.keySet()) {
            scores.put(pid, playerEggCards.get(pid).stream().mapToInt(Integer::intValue).sum());
            eggCounts.put(pid, playerEggCards.get(pid).size());
        }

        int sum = currentRolls.stream().mapToInt(Integer::intValue).sum();

        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("turnId", actualTurnPlayer);
        snapshot.put("playing", playing);
        snapshot.put("positions", new HashMap<>(turtlePositions));
        snapshot.put("positionStacks", new HashMap<>(positionStacks));
        snapshot.put("scores", scores);
        snapshot.put("eggCounts", eggCounts);
        snapshot.put("currentRolls", new ArrayList<>(currentRolls));
        snapshot.put("diceCount", diceCount);
        snapshot.put("currentEggCard", currentEggCard);
        snapshot.put("eggDeckSize", eggDeck.size());
        snapshot.put("lastCardTaken", lastCardTaken);
        snapshot.put("mustRoll", mustRoll);
        snapshot.put("nicknames", nicknames);
        snapshot.put("turnOrder", turnOrder);
        snapshot.put("currentSum", sum);
        snapshot.put("previewMove", diceCount > 0 ? sum * diceCount : 0);

        return snapshot;
    }

    private String getNickname(String id) {
        return users.containsKey(id) ? users.get(id).getNickname() : "Unknown";
    }

    private GameMessage makeMessage(String type, String content) {
        GameMessage msg = new GameMessage();
        msg.setType(type);
        msg.setRoomId(roomId);
        msg.setContent(content);
        msg.setData(getGameSnapshot());
        return msg;
    }

    private GameMessage makeChat(String sender, String content) {
        return GameMessage.builder().type("CHAT").roomId(roomId).sender(sender).content(content).build();
    }
}
const Mahe = (function() {
    const TRACK_LENGTH = 21;
    const RAFT_POSITION = -1;
    // 거북이 색상 (흑백 테마에 맞게)
    const TURTLE_COLORS = ['#e74c3c', '#3498db', '#2ecc71', '#f39c12', '#9b59b6', '#1abc9c'];

    function onEnterRoom() {}

    function onCloseRanking() {
        document.getElementById('mahe-board').classList.add('hidden');
        document.getElementById('startBtn').style.display = 'inline-block';
    }

    function handleMessage(msg, myId) {
        if (msg.data) updateBoard(msg.data, myId);
    }

    function initTrack() {
        const track = document.getElementById('track');
        if (!track) return;
        track.innerHTML = '';

        const raft = document.createElement('div');
        raft.className = 'track-cell raft';
        raft.id = 'cell-raft';
        raft.innerHTML = `<span class="cell-number">🚣</span><span class="cell-label">뗏목</span>`;
        track.appendChild(raft);

        for (let i = 1; i <= TRACK_LENGTH; i++) {
            const cell = document.createElement('div');
            cell.className = 'track-cell' + (i === 21 ? ' finish' : '');
            cell.id = 'cell-' + i;
            cell.innerHTML = i === 21
                ? `<span class="cell-number">🏁</span><span class="cell-label">21</span>`
                : `<span class="cell-number">${i}</span>`;
            track.appendChild(cell);
        }
    }

    function updateBoard(data, myId) {
        document.getElementById('mahe-board').classList.remove('hidden');

        const { playing, turnId, positions = {}, positionStacks = {}, scores = {}, eggCounts = {},
            nicknames = {}, currentRolls = [], diceCount = 0, currentSum = 0, previewMove = 0,
            currentEggCard = 0, eggDeckSize = 0, lastCardTaken = false, mustRoll = true, turnOrder = [] } = data;

        document.getElementById('game-status').textContent = playing ? '진행 중' : '대기 중';
        document.getElementById('startBtn').style.display = playing ? 'none' : 'inline-block';

        const eggInfo = document.getElementById('egg-info');
        eggInfo.innerHTML = lastCardTaken
            ? `<span class="egg-card bonus">🥚 7점 보너스!</span><span class="deck-count">남은: 0장</span>`
            : `<span class="egg-card">🥚 ${currentEggCard}점</span><span class="deck-count">남은: ${eggDeckSize}장</span>`;

        initTrack();

        const panel = document.getElementById('players-panel');
        panel.innerHTML = '';

        turnOrder.forEach((pid, idx) => {
            const nick = nicknames[pid] || pid;
            const score = scores[pid] || 0;
            const eggs = eggCounts[pid] || 0;
            const pos = positions[pid];
            const isActive = pid === turnId;
            const isMe = pid === myId;
            const color = TURTLE_COLORS[idx % TURTLE_COLORS.length];

            const card = document.createElement('div');
            card.className = 'player-card' + (isActive ? ' active-turn' : '') + (isMe ? ' is-me' : '');
            card.innerHTML = `
                <div class="player-turtle" style="color: ${color}">🐢</div>
                <div class="player-name">${nick}${isMe ? ' (나)' : ''}</div>
                <div class="player-score">🥚 ${score}점 (${eggs}장)</div>
                <div class="player-pos">📍 ${pos === RAFT_POSITION ? '뗏목' : pos + '칸'}</div>
            `;
            panel.appendChild(card);
        });

        // 거북이 색상 적용해서 트랙에 배치
        Object.entries(positionStacks).forEach(([posStr, stack]) => {
            const pos = parseInt(posStr);
            const cellId = pos === RAFT_POSITION ? 'cell-raft' : 'cell-' + pos;
            const cell = document.getElementById(cellId);

            if (cell && stack?.length > 0) {
                const container = document.createElement('div');
                container.className = 'turtle-stack';

                stack.forEach((pid, stackIdx) => {
                    const isActive = pid === turnId;
                    const playerIdx = turnOrder.indexOf(pid);
                    const color = TURTLE_COLORS[playerIdx % TURTLE_COLORS.length];

                    const turtle = document.createElement('span');
                    turtle.className = 'turtle' + (isActive ? ' active' : '');
                    turtle.style.position = 'absolute';
                    turtle.style.bottom = (stackIdx * 12) + 'px';
                    turtle.style.color = color;
                    turtle.textContent = '🐢';
                    turtle.title = nicknames[pid] || pid;
                    container.appendChild(turtle);
                });

                cell.appendChild(container);
            }
        });

        const diceDisplay = document.getElementById('dice-display');
        if (currentRolls.length > 0) {
            const faces = ['', '⚀', '⚁', '⚂', '⚃', '⚄', '⚅'];
            diceDisplay.innerHTML = currentRolls.map(d => `<div class="dice">${faces[d]}</div>`).join('') +
                `<div class="dice-info">합: ${currentSum} | ${diceCount}개 → ${previewMove}칸</div>`;
        } else {
            diceDisplay.innerHTML = '<span style="color: var(--text-secondary);">주사위를 굴리세요!</span>';
        }

        const isMyTurn = turnId === myId;
        const actionBtns = document.getElementById('action-btns');
        const turnMsg = document.getElementById('turn-message');
        const rollBtn = document.getElementById('roll-btn');
        const stopBtn = document.getElementById('stop-btn');

        if (playing && isMyTurn) {
            actionBtns.style.visibility = 'visible';
            rollBtn.disabled = diceCount >= 3 || currentSum >= 7;
            stopBtn.disabled = mustRoll;
            turnMsg.textContent = mustRoll ? '🎯 주사위를 굴리세요! (필수)' : `🎯 계속? 멈추기? (${previewMove}칸 이동)`;
        } else if (playing) {
            actionBtns.style.visibility = 'hidden';
            turnMsg.textContent = `⏳ ${nicknames[turnId] || '???'}님 턴...`;
        } else {
            actionBtns.style.visibility = 'hidden';
            turnMsg.textContent = '';
        }
    }

    return {
        onEnterRoom,
        onCloseRanking,
        handleMessage,
        roll: () => Core.sendAction({ actionType: 'ROLL' }),
        stop: () => Core.sendAction({ actionType: 'STOP' })
    };
})();

document.addEventListener('DOMContentLoaded', () => Core.init(Mahe, { gameName: '마헤' }));
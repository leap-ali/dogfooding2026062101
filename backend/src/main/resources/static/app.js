const SOCKET_URL = 'http://localhost:9092';

let socket;
let myPlayerId = null;
let gameState = null;
let selectedCardIds = [];
let declareSuit = null;

document.addEventListener('DOMContentLoaded', function() {
    document.getElementById('joinBtn').addEventListener('click', joinRoom);
    document.getElementById('nicknameInput').addEventListener('keypress', function(e) {
        if (e.key === 'Enter') joinRoom();
    });
    document.getElementById('leaveBtn').addEventListener('click', leaveRoom);
    document.getElementById('declareBtn').addEventListener('click', showDeclareModal);
    document.getElementById('skipDeclareBtn').addEventListener('click', skipDeclare);
    document.getElementById('confirmBottomBtn').addEventListener('click', confirmBottom);
    document.getElementById('playBtn').addEventListener('click', playCards);
    document.getElementById('passBtn').addEventListener('click', passCards);
    document.getElementById('closeDeclareModal').addEventListener('click', closeDeclareModal);
    
    document.querySelectorAll('.suit-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            declareSuit = this.dataset.suit;
            doDeclareTrump();
        });
    });
});

function joinRoom() {
    const nickname = document.getElementById('nicknameInput').value.trim();
    if (!nickname) {
        showLoginError('请输入昵称');
        return;
    }
    
    showLoginError('正在连接...');
    
    socket = io(SOCKET_URL, {
        transports: ['websocket', 'polling'],
        reconnection: true,
        reconnectionAttempts: 5,
        reconnectionDelay: 1000
    });
    
    socket.on('connect', function() {
        console.log('已连接到服务器');
        
        socket.emit('joinRoom', { nickname: nickname }, function(success, result) {
            if (success) {
                myPlayerId = result;
                showGamePage();
                setupSocketListeners();
            } else {
                showLoginError(result);
                socket.disconnect();
            }
        });
    });
    
    socket.on('connect_error', function(error) {
        console.log('连接错误:', error);
        showLoginError('连接服务器失败，请检查服务是否启动');
    });
    
    socket.on('disconnect', function(reason) {
        console.log('断开连接:', reason);
        if (myPlayerId) {
            showMessage('与服务器断开连接');
        }
    });
}

function setupSocketListeners() {
    socket.on('gameState', function(state) {
        gameState = state;
        renderGame();
    });
    
    socket.on('playerJoined', function(nickname) {
        showMessage(nickname + ' 加入了房间');
    });
    
    socket.on('playerLeft', function(nickname) {
        showMessage(nickname + ' 离开了房间');
    });
    
    socket.on('trumpDeclared', function(nickname, suitSymbol) {
        showMessage(nickname + ' 亮主: ' + suitSymbol);
    });
    
    socket.on('bottomDiscarded', function(nickname) {
        showMessage(nickname + ' 扣底完成');
    });
    
    socket.on('cardsPlayed', function(nickname, count) {
        showMessage(nickname + ' 出牌 ' + count + ' 张');
    });
    
    socket.on('error', function(msg) {
        showMessage('错误: ' + msg);
    });
    
    socket.emit('getGameState', function(state) {
        if (state) {
            gameState = state;
            renderGame();
        }
    });
}

function showGamePage() {
    document.getElementById('loginPage').classList.add('hidden');
    document.getElementById('gamePage').classList.remove('hidden');
}

function showLoginError(msg) {
    document.getElementById('loginError').textContent = msg;
}

function showMessage(msg) {
    const msgArea = document.getElementById('messageArea');
    msgArea.textContent = msg;
    msgArea.style.opacity = 1;
    setTimeout(() => {
        msgArea.style.opacity = 0.7;
    }, 2000);
}

function renderGame() {
    if (!gameState) return;
    
    const phaseBadge = document.getElementById('phaseBadge');
    phaseBadge.textContent = gameState.phaseDesc;
    phaseBadge.className = 'phase-badge';
    if (gameState.phase === 'PLAYING') {
        phaseBadge.classList.add('playing');
    } else if (gameState.phase === 'WAITING') {
        phaseBadge.classList.add('waiting');
    }
    
    document.getElementById('trumpInfo').textContent = '主花色: ' + (gameState.trumpSuitSymbol || '-');
    document.getElementById('levelInfo').textContent = '等级: ' + gameState.levelRank;
    document.getElementById('roundInfo').textContent = '第 ' + gameState.roundNumber + ' 轮';
    
    const players = gameState.players || [];
    
    let myPosition = gameState.myPosition;
    if (myPosition === undefined || myPosition === null) {
        for (let i = 0; i < players.length; i++) {
            if (players[i].id === myPlayerId) {
                myPosition = i;
                break;
            }
        }
    }
    
    const positionMap = getPositionMap(myPosition);
    
    for (let i = 0; i < 4; i++) {
        const actualPos = positionMap[i];
        const player = players[actualPos];
        const slot = document.getElementById('player' + i);
        const nameEl = document.getElementById('name' + i);
        const countEl = document.getElementById('count' + i);
        const playedEl = document.getElementById('played' + i);
        
        slot.classList.remove('current-turn', 'is-banker', 'is-me');
        
        if (player) {
            nameEl.textContent = player.nickname + (i === 0 ? ' (我)' : '');
            countEl.textContent = player.handCardCount + ' 张';
            
            if (player.isBanker) {
                slot.classList.add('is-banker');
            }
            
            if (gameState.currentPlayerPosition === actualPos) {
                slot.classList.add('current-turn');
            }
            
            if (i === 0) {
                slot.classList.add('is-me');
            }
            
            const playedCards = getPlayedCardsForPosition(actualPos);
            renderPlayedCards(playedEl, playedCards);
        } else {
            nameEl.textContent = '等待加入...';
            countEl.textContent = '-';
            playedEl.innerHTML = '';
        }
    }
    
    renderHandCards();
    renderActionButtons();
}

function getPositionMap(myPos) {
    if (myPos === undefined || myPos === null || myPos < 0) {
        return [0, 1, 2, 3];
    }
    const map = [];
    map[0] = myPos;
    map[1] = (myPos + 1) % 4;
    map[2] = (myPos + 2) % 4;
    map[3] = (myPos + 3) % 4;
    return map;
}

function getPlayedCardsForPosition(position) {
    if (!gameState.currentRound) return [];
    const played = gameState.currentRound.find(p => p.position === position);
    return played ? played.cards : [];
}

function renderPlayedCards(container, cards) {
    container.innerHTML = '';
    if (!cards || cards.length === 0) return;
    
    cards.forEach(card => {
        const cardEl = document.createElement('div');
        cardEl.className = 'played-card';
        
        if (isRedCard(card)) {
            cardEl.classList.add('red');
        }
        
        if (isTrumpCard(card)) {
            cardEl.classList.add('trump');
        }
        
        cardEl.textContent = getCardDisplayText(card);
        container.appendChild(cardEl);
    });
}

function isRedCard(card) {
    return card.suit === 'HEART' || card.suit === 'DIAMOND' || card.suit === 'JOKER';
}

function isTrumpCard(card) {
    if (!gameState || !gameState.trumpSuit) return false;
    if (card.suit === 'JOKER') return true;
    const levelRank = levelRankToValue(gameState.levelRank);
    if (card.rank === levelRank) return true;
    return card.suit === gameState.trumpSuit;
}

function levelRankToValue(levelStr) {
    const rankMap = {
        '2': 'TWO', '3': 'THREE', '4': 'FOUR', '5': 'FIVE', '6': 'SIX',
        '7': 'SEVEN', '8': 'EIGHT', '9': 'NINE', '10': 'TEN',
        'J': 'JACK', 'Q': 'QUEEN', 'K': 'KING', 'A': 'ACE'
    };
    return rankMap[levelStr] || 'TWO';
}

function getCardDisplayText(card) {
    if (card.suit === 'JOKER') {
        return card.rank === 'BIG_JOKER' ? '大王' : '小王';
    }
    const suitMap = {
        'SPADE': '♠', 'HEART': '♥', 'CLUB': '♣', 'DIAMOND': '♦'
    };
    const rankMap = {
        'TWO': '2', 'THREE': '3', 'FOUR': '4', 'FIVE': '5', 'SIX': '6',
        'SEVEN': '7', 'EIGHT': '8', 'NINE': '9', 'TEN': '10',
        'JACK': 'J', 'QUEEN': 'Q', 'KING': 'K', 'ACE': 'A'
    };
    return (suitMap[card.suit] || '') + (rankMap[card.rank] || '');
}

function renderHandCards() {
    const container = document.getElementById('handCards');
    container.innerHTML = '';
    
    if (!gameState || !gameState.myCards || gameState.myCards.length === 0) {
        return;
    }
    
    const myCards = gameState.myCards.slice();
    myCards.sort((a, b) => sortCards(a, b));
    
    myCards.forEach(card => {
        const cardEl = document.createElement('div');
        cardEl.className = 'card';
        cardEl.dataset.cardId = card.id;
        
        if (isRedCard(card)) {
            cardEl.classList.add('red');
        }
        
        if (isTrumpCard(card)) {
            cardEl.classList.add('trump');
        }
        
        if (selectedCardIds.includes(card.id)) {
            cardEl.classList.add('selected');
        }
        
        const displayText = getCardDisplayText(card);
        cardEl.innerHTML = '<div class="card-suit">' + displayText + '</div>';
        
        cardEl.addEventListener('click', function() {
            toggleCardSelection(card.id);
        });
        
        container.appendChild(cardEl);
    });
}

function sortCards(a, b) {
    const aTrump = isTrumpCard(a);
    const bTrump = isTrumpCard(b);
    if (aTrump && !bTrump) return -1;
    if (!aTrump && bTrump) return 1;
    
    const suitOrder = { 'SPADE': 4, 'HEART': 3, 'CLUB': 2, 'DIAMOND': 1, 'JOKER': 5 };
    const rankOrder = { 'TWO': 2, 'THREE': 3, 'FOUR': 4, 'FIVE': 5, 'SIX': 6, 'SEVEN': 7, 
                       'EIGHT': 8, 'NINE': 9, 'TEN': 10, 'JACK': 11, 'QUEEN': 12, 'KING': 13, 
                       'ACE': 14, 'SMALL_JOKER': 16, 'BIG_JOKER': 17 };
    
    const suitA = suitOrder[a.suit] || 0;
    const suitB = suitOrder[b.suit] || 0;
    if (suitA !== suitB) return suitB - suitA;
    
    const rankA = rankOrder[a.rank] || 0;
    const rankB = rankOrder[b.rank] || 0;
    return rankB - rankA;
}

function toggleCardSelection(cardId) {
    if (gameState.phase !== 'PLAYING' && gameState.phase !== 'BOTTOM_CARDS') {
        return;
    }
    
    const myPos = gameState.myPosition;
    if (gameState.phase === 'PLAYING' && gameState.currentPlayerPosition !== myPos) {
        return;
    }
    
    const index = selectedCardIds.indexOf(cardId);
    if (index > -1) {
        selectedCardIds.splice(index, 1);
    } else {
        if (gameState.phase === 'BOTTOM_CARDS') {
            if (selectedCardIds.length >= 8) {
                selectedCardIds.shift();
            }
        }
        selectedCardIds.push(cardId);
    }
    
    renderHandCards();
    renderActionButtons();
}

function renderActionButtons() {
    const declareBtn = document.getElementById('declareBtn');
    const skipDeclareBtn = document.getElementById('skipDeclareBtn');
    const confirmBottomBtn = document.getElementById('confirmBottomBtn');
    const playBtn = document.getElementById('playBtn');
    const passBtn = document.getElementById('passBtn');
    
    declareBtn.classList.add('hidden');
    skipDeclareBtn.classList.add('hidden');
    confirmBottomBtn.classList.add('hidden');
    playBtn.classList.add('hidden');
    passBtn.classList.add('hidden');
    
    if (!gameState || gameState.phase === 'WAITING') {
        return;
    }
    
    const myPos = gameState.myPosition;
    const isMyTurn = gameState.currentPlayerPosition === myPos;
    
    if (gameState.phase === 'DECLARE_TRUMP' && isMyTurn) {
        declareBtn.classList.remove('hidden');
        skipDeclareBtn.classList.remove('hidden');
        
        const myCards = gameState.myCards || [];
        const levelVal = levelRankToValue(gameState.levelRank);
        const hasLevelCard = myCards.some(c => c.rank === levelVal && c.suit !== 'JOKER');
        declareBtn.disabled = !hasLevelCard;
        if (hasLevelCard) {
            declareBtn.classList.remove('btn-disabled');
        }
    }
    
    if (gameState.phase === 'BOTTOM_CARDS') {
        const myPlayer = (gameState.players || []).find(p => p.position === myPos);
        if (myPlayer && myPlayer.isBanker) {
            confirmBottomBtn.classList.remove('hidden');
            confirmBottomBtn.disabled = selectedCardIds.length !== 8;
            if (selectedCardIds.length === 8) {
                confirmBottomBtn.textContent = '确认扣底 (' + selectedCardIds.length + '/8)';
            } else {
                confirmBottomBtn.textContent = '选择扣底牌 (' + selectedCardIds.length + '/8)';
            }
        }
    }
    
    if (gameState.phase === 'PLAYING' && isMyTurn) {
        playBtn.classList.remove('hidden');
        playBtn.disabled = selectedCardIds.length === 0;
        if (selectedCardIds.length > 0) {
            playBtn.textContent = '出牌 (' + selectedCardIds.length + '张)';
        } else {
            playBtn.textContent = '出牌';
        }
    }
}

function showDeclareModal() {
    document.getElementById('declareModal').classList.remove('hidden');
}

function closeDeclareModal() {
    document.getElementById('declareModal').classList.add('hidden');
    declareSuit = null;
}

function doDeclareTrump() {
    if (!declareSuit) return;
    
    socket.emit('declareTrump', { suit: declareSuit }, function(success, msg) {
        if (!success) {
            showMessage('亮主失败: ' + msg);
        }
        closeDeclareModal();
    });
}

function skipDeclare() {
    socket.emit('skipDeclare', function(success, msg) {
        if (!success && msg) {
            showMessage('操作失败: ' + msg);
        }
    });
}

function confirmBottom() {
    if (selectedCardIds.length !== 8) {
        showMessage('请选择8张牌扣底');
        return;
    }
    
    socket.emit('discardBottom', { cardIds: selectedCardIds }, function(success, msg) {
        if (success) {
            selectedCardIds = [];
        } else {
            showMessage('扣底失败: ' + msg);
        }
    });
}

function playCards() {
    if (selectedCardIds.length === 0) {
        showMessage('请选择要出的牌');
        return;
    }
    
    socket.emit('playCards', { cardIds: selectedCardIds }, function(success, msg) {
        if (success) {
            selectedCardIds = [];
        } else {
            showMessage('出牌失败: ' + msg);
        }
    });
}

function passCards() {
    selectedCardIds = [];
    renderHandCards();
    renderActionButtons();
}

function leaveRoom() {
    if (confirm('确定要退出房间吗？退出后对局将结束。')) {
        if (socket) {
            socket.emit('leaveRoom', function() {
                socket.disconnect();
                location.reload();
            });
        }
    }
}

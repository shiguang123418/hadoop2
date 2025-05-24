document.addEventListener('DOMContentLoaded', function() {
    // Elements
    const serverUrlInput = document.getElementById('server-url');
    const statusIndicator = document.getElementById('status-indicator');
    const statusText = document.getElementById('status-text');
    const connectBtn = document.getElementById('connect-btn');
    const disconnectBtn = document.getElementById('disconnect-btn');
    const messageInput = document.getElementById('message');
    const sendBtn = document.getElementById('send-btn');
    const messageLog = document.getElementById('message-log');
    
    // WebSocket instance
    let socket = null;
    
    // Initialize UI
    function initUI() {
        connectBtn.addEventListener('click', connect);
        disconnectBtn.addEventListener('click', disconnect);
        sendBtn.addEventListener('click', sendMessage);
        
        // Handle Enter key in message input
        messageInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                sendMessage();
            }
        });
        
        // Add a system message
        addMessage('System initialized. Click "Connect" to start.', 'system');
        
        // 检测SockJS是否正确加载
        if (typeof SockJS === 'undefined') {
            addMessage('警告: SockJS 库未加载!', 'system');
            console.error('SockJS library not loaded');
        } else {
            addMessage('SockJS 库已加载，版本: ' + SockJS.version, 'system');
        }
    }
    
    // Connect to WebSocket server using SockJS
    function connect() {
        let serverUrl = serverUrlInput.value.trim();
        
        if (!serverUrl) {
            addMessage('请输入有效的WebSocket URL.', 'system');
            return;
        }
        
        try {
            // Update UI state
            setConnectionStatus('connecting');
            
            // 确保URL使用http或https协议
            if (serverUrl.startsWith('ws:') || serverUrl.startsWith('wss:')) {
                let urlObj = new URL(serverUrl);
                let protocol = urlObj.protocol === 'ws:' ? 'http:' : 'https:';
                serverUrl = protocol + '//' + urlObj.host + urlObj.pathname;
                addMessage(`已将URL从WebSocket协议转换为HTTP协议: ${serverUrl}`, 'system');
            }
            
            addMessage(`尝试连接到: ${serverUrl}`, 'system');
            console.log(`Connecting to: ${serverUrl}`);
            
            // Create new SockJS connection
            socket = new SockJS(serverUrl);
            
            // WebSocket event listeners
            socket.onopen = function(event) {
                setConnectionStatus('connected');
                addMessage(`连接成功! 传输类型: ${socket._transport ? socket._transport.transportName : '未知'}`, 'system');
                console.log('Connection established', socket);
                enableMessageControls(true);
            };
            
            socket.onmessage = function(event) {
                console.log('Received message:', event.data);
                addMessage(event.data, 'received');
            };
            
            socket.onerror = function(event) {
                addMessage('连接错误. 详情请查看控制台.', 'system');
                console.error('WebSocket error:', event);
            };
            
            socket.onclose = function(event) {
                setConnectionStatus('disconnected');
                addMessage(`连接关闭. Code: ${event.code}, 原因: ${event.reason || '没有提供原因'}`, 'system');
                console.log('Connection closed:', event);
                enableMessageControls(false);
            };
            
            // 监听服务器心跳状态
            const heartbeatInterval = setInterval(function() {
                if (socket && socket.readyState === SockJS.OPEN) {
                    console.log('WebSocket连接状态: ' + socket.readyState);
                } else if (!socket || socket.readyState !== SockJS.OPEN) {
                    console.log('WebSocket连接已关闭或不可用');
                    clearInterval(heartbeatInterval);
                }
            }, 10000); // 每10秒检查一次
            
        } catch (error) {
            setConnectionStatus('disconnected');
            addMessage(`连接失败: ${error.message}`, 'system');
            console.error('Connection error:', error);
        }
    }
    
    // Disconnect from WebSocket server
    function disconnect() {
        if (socket !== null) {
            socket.close();
            addMessage('断开连接...', 'system');
        }
    }
    
    // Send a message to the server
    function sendMessage() {
        const message = messageInput.value.trim();
        
        if (!message) {
            return;
        }
        
        if (socket && socket.readyState === SockJS.OPEN) {
            socket.send(message);
            addMessage(message, 'sent');
            messageInput.value = '';
        } else {
            addMessage('未连接到服务器.', 'system');
            console.log('Socket readyState:', socket ? socket.readyState : 'null');
        }
    }
    
    // Add a message to the message log
    function addMessage(message, type) {
        const messageElement = document.createElement('div');
        messageElement.classList.add('message', type);
        
        const timestamp = document.createElement('div');
        timestamp.classList.add('timestamp');
        timestamp.textContent = new Date().toLocaleTimeString();
        
        const content = document.createElement('div');
        content.classList.add('message-content');
        content.textContent = message;
        
        messageElement.appendChild(timestamp);
        messageElement.appendChild(content);
        
        messageLog.appendChild(messageElement);
        messageLog.scrollTop = messageLog.scrollHeight; // Auto-scroll to bottom
        
        // 同时记录到控制台
        console.log(`[${type}] ${message}`);
    }
    
    // Set connection status and update UI
    function setConnectionStatus(status) {
        statusIndicator.className = status;
        
        switch (status) {
            case 'connected':
                statusText.textContent = '已连接';
                connectBtn.disabled = true;
                disconnectBtn.disabled = false;
                break;
            case 'disconnected':
                statusText.textContent = '未连接';
                connectBtn.disabled = false;
                disconnectBtn.disabled = true;
                socket = null;
                break;
            case 'connecting':
                statusText.textContent = '连接中...';
                connectBtn.disabled = true;
                disconnectBtn.disabled = true;
                break;
        }
    }
    
    // Enable or disable message controls
    function enableMessageControls(enabled) {
        messageInput.disabled = !enabled;
        sendBtn.disabled = !enabled;
        
        if (enabled) {
            messageInput.focus();
        }
    }
    
    // Initialize the UI
    initUI();
});
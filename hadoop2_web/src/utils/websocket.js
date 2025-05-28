import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'

/**
 * WebSocket连接管理工具
 * 提供WebSocket连接、断开、订阅等功能
 */
class WebSocketManager {
  constructor() {
    this.client = null
    this.subscriptions = {}
    this.connected = false
    this.listeners = {
      connect: [],
      disconnect: [],
      error: [],
      message: []
    }
  }

  /**
   * 连接到WebSocket服务器
   * @returns {Promise} 连接Promise
   */
  connect() {
    return new Promise((resolve, reject) => {
      if (this.client && this.connected) {
        console.log('WebSocket已连接，跳过重复连接')
        resolve(this.client)
        return
      }

      try {
        // 根据环境确定WebSocket连接地址
        let wsUrl
        if (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') {
          // 本地开发环境，使用代理
          wsUrl = '/api_ws'
        } else if (window.location.hostname === '192.168.1.192') {
          // 开发服务器环境，直接连接
          wsUrl = 'http://192.168.1.192:8001/api/ws'
        } else {
          // 其他环境，基于当前域名构建WebSocket地址
          const protocol = window.location.protocol === 'https:' ? 'https:' : 'http:'
          const host = window.location.hostname
          wsUrl = `${protocol}//${host}:8001/api/ws`
        }

        console.log('使用WebSocket连接地址:', wsUrl)
        const socket = new SockJS(wsUrl)

        this.client = new Client({
          webSocketFactory: () => socket,
          debug: function(str) {
            console.log('STOMP: ' + str)
          },
          onConnect: frame => {
            this.connected = true
            console.log('WebSocket连接成功:', frame)
            
            // 触发连接事件
            this._triggerEvent('connect', frame)
            
            resolve(this.client)
          },
          onStompError: error => {
            console.error('STOMP错误:', error)
            this.connected = false
            
            // 触发错误事件
            this._triggerEvent('error', {
              type: 'stomp',
              error: error,
              message: error.headers?.message || '未知STOMP错误'
            })
            
            reject(error)
          }
        })

        this.client.activate()
      } catch (e) {
        console.error('创建WebSocket连接异常:', e)
        this._triggerEvent('error', {
          type: 'connection',
          error: e,
          message: e.message || '创建WebSocket连接失败'
        })
        reject(e)
      }
    })
  }

  /**
   * 断开WebSocket连接
   */
  disconnect() {
    if (!this.client || !this.connected) {
      console.log('WebSocket未连接，无需断开')
      return
    }

    // 取消所有订阅
    Object.values(this.subscriptions).forEach(subscription => {
      if (subscription && typeof subscription.unsubscribe === 'function') {
        subscription.unsubscribe()
      }
    })
    this.subscriptions = {}

    // 断开连接
    this.client.deactivate()
    this.client = null
    this.connected = false
    
    // 触发断开连接事件
    this._triggerEvent('disconnect')
  }

  /**
   * 订阅主题
   * @param {string} topic 主题
   * @param {Function} callback 回调函数
   * @returns {Object} 订阅对象
   */
  subscribe(topic, callback) {
    return new Promise((resolve, reject) => {
      if (!this.client || !this.connected) {
        this._triggerEvent('error', {
          type: 'subscription',
          message: 'WebSocket未连接，请先连接'
        })
        reject(new Error('WebSocket未连接，请先连接'))
        return
      }

      // 如果已订阅，先取消订阅
      if (this.subscriptions[topic]) {
        this.subscriptions[topic].unsubscribe()
        delete this.subscriptions[topic]
      }

      try {
        // 订阅新主题
        const subscription = this.client.subscribe(topic, message => {
          try {
            const data = JSON.parse(message.body)
            
            // 触发消息事件
            this._triggerEvent('message', {
              topic,
              data,
              rawMessage: message
            })
            
            // 调用回调函数
            if (typeof callback === 'function') {
              callback(data, message)
            }
          } catch (e) {
            console.error(`处理主题 ${topic} 消息出错:`, e)
            this._triggerEvent('error', {
              type: 'message',
              topic,
              error: e,
              message: e.message || '处理消息失败'
            })
          }
        })

        this.subscriptions[topic] = subscription
        console.log(`已订阅主题: ${topic}`)
        resolve(subscription)
      } catch (e) {
        console.error(`订阅主题 ${topic} 失败:`, e)
        this._triggerEvent('error', {
          type: 'subscription',
          topic,
          error: e,
          message: e.message || '订阅失败'
        })
        reject(e)
      }
    })
  }

  /**
   * 取消订阅主题
   * @param {string} topic 主题
   */
  unsubscribe(topic) {
    if (this.subscriptions[topic]) {
      this.subscriptions[topic].unsubscribe()
      delete this.subscriptions[topic]
      console.log(`已取消订阅: ${topic}`)
    }
  }

  /**
   * 判断是否已连接
   * @returns {boolean} 是否已连接
   */
  isConnected() {
    return this.connected
  }

  /**
   * 添加事件监听器
   * @param {string} event 事件名称 (connect|disconnect|error|message)
   * @param {Function} callback 回调函数
   */
  on(event, callback) {
    if (this.listeners[event]) {
      this.listeners[event].push(callback)
    }
  }

  /**
   * 移除事件监听器
   * @param {string} event 事件名称
   * @param {Function} callback 回调函数
   */
  off(event, callback) {
    if (this.listeners[event]) {
      this.listeners[event] = this.listeners[event].filter(cb => cb !== callback)
    }
  }

  /**
   * 触发事件
   * @param {string} event 事件名称
   * @param {any} data 事件数据
   * @private
   */
  _triggerEvent(event, data) {
    if (this.listeners[event]) {
      this.listeners[event].forEach(callback => {
        try {
          callback(data)
        } catch (e) {
          console.error(`执行 ${event} 事件回调出错:`, e)
        }
      })
    }
  }
}

// 导出单例
const websocketManager = new WebSocketManager()
export default websocketManager 
// 工具函数
const Utils = {
  // 生成UUID
  generateUUID() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
      var r = (Math.random() * 16) | 0,
        v = c == 'x' ? r : (r & 0x3) | 0x8
      return v.toString(16)
    })
  },

  // 复制到剪贴板
  async copyToClipboard(text) {
    try {
      await navigator.clipboard.writeText(text)
      this.showNotification('已复制到剪贴板', 'success')
    } catch (err) {
      console.error('复制失败:', err)
      // 降级方案
      const textArea = document.createElement('textarea')
      textArea.value = text
      document.body.appendChild(textArea)
      textArea.select()
      try {
        document.execCommand('copy')
        this.showNotification('已复制到剪贴板', 'success')
      } catch (err) {
        this.showNotification('复制失败，请手动复制', 'error')
      }
      document.body.removeChild(textArea)
    }
  },

  // 显示通知
  showNotification(message, type = 'success') {
    // 移除现有通知
    const existingNotification = document.querySelector('.notification')
    if (existingNotification) {
      existingNotification.remove()
    }

    // 创建新通知
    const notification = document.createElement('div')
    notification.className = `notification ${type}`
    notification.innerHTML = `
            <div class="flex items-center">
                <i class="fas ${type === 'success' ? 'fa-check-circle' : 'fa-exclamation-circle'} mr-2"></i>
                <span>${message}</span>
            </div>
        `

    document.body.appendChild(notification)

    // 3秒后自动移除
    setTimeout(() => {
      if (notification.parentNode) {
        notification.remove()
      }
    }, 3000)
  },

  // 格式化文件大小
  formatFileSize(bytes) {
    if (bytes === 0) return '0 B'
    const k = 1024
    const sizes = ['B', 'KB', 'MB', 'GB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
  },

  // 设置产品图标
  setProductIcon(element, iconClass) {
    if (iconClass && iconClass.startsWith('icon-')) {
      const iconName = iconClass.replace('icon-', '')
      element.style.backgroundImage = `url('/images/${iconName}.svg')`
      element.className = 'product-icon mx-auto mb-4 rounded-2xl'
    }
  },

  // 防抖函数
  debounce(func, wait) {
    let timeout
    return function executedFunction(...args) {
      const later = () => {
        clearTimeout(timeout)
        func(...args)
      }
      clearTimeout(timeout)
      timeout = setTimeout(later, wait)
    }
  },

  // 格式化日期
  formatDate(date) {
    return new Date(date).toLocaleDateString('zh-CN')
  },

  // 验证日期格式
  isValidDate(dateString) {
    const regex = /^\d{4}-\d{2}-\d{2}$/
    if (!regex.test(dateString)) return false
    const date = new Date(dateString)
    return date instanceof Date && !isNaN(date)
  },

  // 获取默认过期日期（一年后）
  getDefaultExpiryDate() {
    const date = new Date()
    date.setFullYear(date.getFullYear() + 1)
    return date.toISOString().split('T')[0]
  },

  // 主题切换功能
  toggleTheme(event) {
    // 检查浏览器是否支持 View Transition API
    if (!document.startViewTransition) {
      // 不支持则直接切换主题，不添加动画
      document.documentElement.classList.toggle('dark')
      this.saveTheme()
      return
    }

    // 标记正在使用View Transition，避免CSS过渡冲突
    document.documentElement.classList.add('view-transition-active')

    const transition = document.startViewTransition(() => {
      document.documentElement.classList.toggle('dark')
      this.saveTheme()
    })

    transition.ready.then(() => {
      const { clientX, clientY } = event

      const endRadius = Math.hypot(Math.max(clientX, innerWidth - clientX), Math.max(clientY, innerHeight - clientY))

      const clipPath = [`circle(0px at ${clientX}px ${clientY}px)`, `circle(${endRadius}px at ${clientX}px ${clientY}px)`]

      const isDark = document.documentElement.classList.contains('dark')

      document.documentElement.animate(
        {
          clipPath: isDark ? clipPath.reverse() : clipPath
        },
        {
          duration: 450,
          easing: 'ease-in',
          pseudoElement: isDark ? '::view-transition-old(root)' : '::view-transition-new(root)'
        }
      )
    })

    // 动画完成后移除标记类
    transition.finished.finally(() => {
      document.documentElement.classList.remove('view-transition-active')
    })
  },

  // 保存主题设置
  saveTheme() {
    const isDark = document.documentElement.classList.contains('dark')
    localStorage.setItem('theme', isDark ? 'dark' : 'light')
  },

  // 加载主题设置
  loadTheme() {
    const savedTheme = localStorage.getItem('theme')
    if (savedTheme === 'dark') {
      document.documentElement.classList.add('dark')
    } else if (savedTheme === 'light') {
      document.documentElement.classList.remove('dark')
    } else {
      // 如果没有保存的主题，检查系统偏好
      const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
      if (prefersDark) {
        document.documentElement.classList.add('dark')
      }
    }
  },

  // 获取当前主题
  getCurrentTheme() {
    return document.documentElement.classList.contains('dark') ? 'dark' : 'light'
  },

  // 路由管理
  getCurrentPage() {
    const hash = window.location.hash.slice(1) // 移除 # 号
    const validPages = ['home', 'products', 'plugins', 'jrebel', 'sponsor']
    return validPages.includes(hash) ? hash : 'home'
  },

  navigateToPage(page) {
    window.location.hash = page
  },

  onHashChange(callback) {
    window.addEventListener('hashchange', callback)
  },

  removeHashChangeListener(callback) {
    window.removeEventListener('hashchange', callback)
  }
}

// API 服务
const ApiService = {
  baseURL: window.location.origin,

  async get(url) {
    try {
      const response = await fetch(`${this.baseURL}${url}`)
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }
      return await response.json()
    } catch (error) {
      console.error('API GET error:', error)
      throw error
    }
  },

  async getText(url) {
    try {
      const response = await fetch(`${this.baseURL}${url}`)
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }
      return await response.text()
    } catch (error) {
      console.error('API GET text error:', error)
      throw error
    }
  },

  // 获取产品列表
  async getProducts() {
    return await this.get('/api/products')
  },

  // 获取插件列表
  async getPlugins() {
    return await this.get('/api/plugins')
  },

  // 生成产品激活码
  async generateLicense(productCode, licenseeName, assigneeName, expiryDate) {
    const params = new URLSearchParams({
      ...(productCode && { productCode }),
      licenseeName,
      assigneeName,
      expiryDate
    })
    return await this.getText(`/license-code/generate?${params}`)
  },

  // 下载代理工具
  downloadAgent() {
    window.open('/ja-netfilter', '_blank')
  }
}

// 存储服务
const StorageService = {
  // 保存配置
  saveConfig(licenseName, assigneeName) {
    localStorage.setItem('licenseName', licenseName)
    localStorage.setItem('assigneeName', assigneeName)
  },

  // 获取配置
  getConfig() {
    return {
      licenseName: localStorage.getItem('licenseName') || '',
      assigneeName: localStorage.getItem('assigneeName') || ''
    }
  },

  // 检查是否已配置
  isConfigured() {
    const config = this.getConfig()
    return config.licenseName && config.assigneeName
  },

  // 清除配置
  clearConfig() {
    localStorage.removeItem('licenseName')
    localStorage.removeItem('assigneeName')
  }
}

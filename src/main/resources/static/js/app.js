// Vue 3 主应用
const { createApp } = Vue

// 主应用组件
const App = {
  data() {
    return {
      currentPage: Utils.getCurrentPage(), // 从URL获取当前页面
      showConfigModal: false,
      showLicenseModal: false,
      showResultModal: false,
      showPowerConfModal: false,
      showUsageModal: false,
      powerConfContent: '',
      isGenerating: false,
      config: {
        licenseName: '',
        assigneeName: ''
      },
      licenseConfig: {
        expiryDate: '',
        licenseType: 'PERPETUAL',
        userCount: 1
      },
      licenseTypes: Object.values(Utils.LicenseType),
      selectedItem: null,
      oldGeneratedLicense: [],
      generatedLicense: '',
      currentPageNum: 1,
      pageSize: 10,
      currentRecordTab: 'code', // 当前记录页面的 Tab: 'code' 或 'plugin'
      products: [],
      plugins: [],
      pluginsUpdateTime: [],
      filteredProducts: [],
      filteredPlugins: [],
      lastPluginUpdateTime: "",
      searchQuery: '',
      navItems: [
        { id: 'home', name: '首页', icon: 'fas fa-home' },
        { id: 'products', name: '产品', icon: 'fas fa-cube' },
        { id: 'plugins', name: '插件', icon: 'fas fa-puzzle-piece' },
        { id: 'records', name: '记录', icon: 'fas fa-history' },
        // { id: 'sponsor', name: '赞助', icon: 'fas fa-heart' }
      ],
      showBackToTop: false
    }
  },

  computed: {
    serverUrl() {
      return `${window.location.origin}`
    },

    jrebelServerUrl() {
      const uuid = Utils.generateUUID()
      return `${window.location.origin}/${uuid}`
    },

    // 排序后的历史记录（按生成时间倒序）
    sortedLicenseHistory() {
      return [...this.oldGeneratedLicense].sort((a, b) => {
        return new Date(b.generationTime) - new Date(a.generationTime);
      });
    },

    // 分页后的历史记录
    paginatedLicenseHistory() {
      const start = (this.currentPageNum - 1) * this.pageSize;
      const end = start + this.pageSize;
      return this.sortedLicenseHistory.slice(start, end);
    },

    // 总页数
    totalPages() {
      return Math.ceil(this.sortedLicenseHistory.length / this.pageSize);
    }
  },

  watch: {
    searchQuery(newQuery) {
      this.filterItems(newQuery)
    },

    currentPage() {
      this.searchQuery = ''
      // 重置过滤结果
      this.filteredProducts = [...this.products]
      this.filteredPlugins = [...this.plugins]
      // 页面切换时滚动到顶部
      this.scrollToTop()
    }
  },

  mounted() {
    this.loadConfig()
    this.loadProducts()
    this.loadPlugins()
    this.loadPluginUpdateTime()
    this.setDefaultExpiryDate()

    // 加载主题设置
    Utils.loadTheme()
    
    // 如果当前页面是记录页面，加载历史记录
    if (this.currentPage === 'records') {
      this.loadLicenseHistory()
    }

    // 监听路由变化
    this.handleHashChange = () => {
      this.currentPage = Utils.getCurrentPage()
      this.searchQuery = ''
      // 重置过滤结果
      this.filteredProducts = [...this.products]
      this.filteredPlugins = [...this.plugins]
      // 切换到记录页面时加载历史记录
      if (this.currentPage === 'records') {
        this.loadLicenseHistory()
      }
    }

    Utils.onHashChange(this.handleHashChange)

    // 监听滚动事件
    const handleScroll = () => {
      const scrollTop = window.pageYOffset || document.documentElement.scrollTop || document.body.scrollTop
      this.showBackToTop = scrollTop > 300
    }

    // 监听滚动事件
    window.addEventListener('scroll', handleScroll)

    // 保存函数引用以便清理
    this._handleScroll = handleScroll
    
    // 监听 ESC 键关闭弹窗
    this._handleKeydown = (event) => {
      if (event.key === 'Escape' || event.keyCode === 27) {
        this.closeAllModals()
      }
    }
    window.addEventListener('keydown', this._handleKeydown)
  },

  beforeUnmount() {
    // 清理路由监听器
    if (this.handleHashChange) {
      Utils.removeHashChangeListener(this.handleHashChange)
    }

    // 清理滚动事件监听器
    if (this._handleScroll) {
      window.removeEventListener('scroll', this._handleScroll)
      document.removeEventListener('scroll', this._handleScroll)
    }
    
    // 清理键盘事件监听器
    if (this._handleKeydown) {
      window.removeEventListener('keydown', this._handleKeydown)
    }
  },

  methods: {
    // 关闭所有弹窗
    closeAllModals() {
      this.showConfigModal = false
      this.showLicenseModal = false
      this.showResultModal = false
      this.showPowerConfModal = false
      this.showUsageModal = false
    },
    
    // 配置相关
    loadConfig() {
      const config = StorageService.getConfig()
      if (StorageService.isConfigured()) {
        this.config = config
      } else {
        this.showConfigModal = true
      }
    },

    saveConfig() {
      if (this.config.licenseName && this.config.assigneeName) {
        StorageService.saveConfig(this.config.licenseName, this.config.assigneeName)
        this.showConfigModal = false
        Utils.showNotification('配置保存成功')
      }
    },

    // 数据加载
    async loadProducts() {
      try {
        this.products = await ApiService.getProducts()
        this.filteredProducts = [...this.products]
      } catch (error) {
        console.error('加载产品列表失败:', error)
        Utils.showNotification('加载产品列表失败', 'error')
      }
    },

    async loadPlugins() {
      try {
        this.plugins = await ApiService.getPlugins()
        this.filteredPlugins = [...this.plugins]
      } catch (error) {
        console.error('加载插件列表失败:', error)
        Utils.showNotification('加载插件列表失败', 'error')
      }
    },

    async loadPluginUpdateTime() {
      try {
        this.pluginsUpdateTime = await ApiService.getPluginUpdateTime()
        // 获取数组最后一个元素的更新时间
        if (this.pluginsUpdateTime && this.pluginsUpdateTime.length > 0) {
          const lastItem = this.pluginsUpdateTime[this.pluginsUpdateTime.length - 1]
          this.lastPluginUpdateTime = lastItem.updateTime || ''
        }
      } catch (error) {
        console.error('加载插件更新时间失败:', error)
      }
    },

    async getPluginUpdateTime() {
      await this.loadPluginUpdateTime()
      Utils.showNotification('更新时间已刷新', 'success')
    },

    // 搜索功能
    filterItems(query) {
      const searchTerm = query.toLowerCase().trim()

      if (this.currentPage === 'products') {
        this.filteredProducts = this.products.filter((product) =>
          product.name.toLowerCase().includes(searchTerm) ||
          (product.description && product.description.toLowerCase().includes(searchTerm))
        )
      } else if (this.currentPage === 'plugins') {
        this.filteredPlugins = this.plugins.filter((plugin) =>
          plugin.name.toLowerCase().includes(searchTerm) ||
          (plugin.description && plugin.description.toLowerCase().includes(searchTerm))
        )
      }
    },

    // 选择产品/插件
    selectProduct(product) {
      this.selectedItem = product
      this.showLicenseModal = true
    },

    selectPlugin(plugin) {
      this.selectedItem = plugin
      this.showLicenseModal = true
    },

    openPluginLink(plugin) {
      // 在新标签页打开插件链接
      if (plugin.link) {
        window.open(plugin.link, '_blank')
      }
    },

    setExpiryDate(days) {
      const date = new Date()
      date.setDate(date.getDate() + parseInt(days))
      this.licenseConfig.expiryDate = date.toISOString().split('T')[0]
    },

    // 生成激活码
    async generateLicense() {
      this.isGenerating = true
      try {
        const result = await ApiService.generateLicense(
            this.selectedItem.productCode,
            this.config.licenseName,
            this.config.assigneeName,
            this.licenseConfig.expiryDate,
            this.licenseConfig.licenseType,
            this.licenseConfig.userCount,
            this.selectedItem.name
        )
        this.generatedLicense = result.activationCode
        
        // 保存激活码历史记录到 localStorage
        const licenseHistory = JSON.parse(localStorage.getItem('licenseHistory') || '[]');
        licenseHistory.push(result);
        localStorage.setItem('licenseHistory', JSON.stringify(licenseHistory));
        
        this.showLicenseModal = false
        this.showResultModal = true
      } catch (error) {
        console.error('生成激活码失败:', error)
        Utils.showNotification('生成激活码失败，请重试', 'error')
      } finally {
        this.isGenerating = false
      }
    },

    async viewPowerConf() {
      try {
        const response = await fetch(`${ApiService.baseURL}/power-conf`)
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`)
        }
        this.powerConfContent = await response.text()
        this.showPowerConfModal = true
      } catch (error) {
        console.error('获取 power.conf 失败:', error)
        Utils.showNotification('获取配置文件失败', 'error')
      }
    },

    // 加载历史记录（用于记录页面）
    loadLicenseHistory() {
      this.oldGeneratedLicense = JSON.parse(localStorage.getItem('licenseHistory') || '[]');
      this.currentPageNum = 1; // 重置到第一页
    },

    // 跳转到指定页
    goToPage(page) {
      if (page >= 1 && page <= this.totalPages) {
        this.currentPageNum = page;
      }
    },

    // 上一页
    prevPage() {
      if (this.currentPageNum > 1) {
        this.currentPageNum--;
      }
    },

    // 下一页
    nextPage() {
      if (this.currentPageNum < this.totalPages) {
        this.currentPageNum++;
      }
    },

    // 工具方法
    downloadAgent() {
      ApiService.downloadAgent()
    },

    copyToClipboard(text) {
      Utils.copyToClipboard(text)
    },

    setDefaultExpiryDate() {
      this.licenseConfig.expiryDate = Utils.getDefaultExpiryDate()
    },

    // 图标处理
    getIcon(item) {
      return item.icon || '/images/plugin.svg'
    },

    // 页面跳转
    navigateTo(page) {
      Utils.navigateToPage(page)
    },

    // 返回顶部
    scrollToTop() {
      window.scrollTo({
        top: 0,
        behavior: 'smooth'
      })
    },

    // 主题切换
    toggleTheme(event) {
      Utils.toggleTheme(event)
    },

    // 格式化日期时间
    formatDate(dateString) {
      if (!dateString) return '';
      const date = new Date(dateString);
      return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
      });
    },

    // 获取许可证类型文本
    getLicenseTypeText(value) {
      return Utils.getLicenseTypeText(value);
    },

    // 删除单条激活码记录
    deleteLicenseRecord(index) {
      if (confirm('确定要删除这条激活码记录吗？')) {
        const licenseHistory = JSON.parse(localStorage.getItem('licenseHistory') || '[]');
        // 获取当前显示的数据（已排序）
        const currentRecord = this.paginatedLicenseHistory[index];
        // 在原始数据中找到该记录的索引
        const originalIndex = licenseHistory.findIndex(item => item.activationCode === currentRecord.activationCode);
        if (originalIndex !== -1) {
          licenseHistory.splice(originalIndex, 1);
          localStorage.setItem('licenseHistory', JSON.stringify(licenseHistory));
          // 更新当前显示的数据
          this.oldGeneratedLicense = licenseHistory;
          // 如果当前页没有数据了，跳转到上一页
          if (this.paginatedLicenseHistory.length === 0 && this.currentPageNum > 1) {
            this.currentPageNum--;
          }
          Utils.showNotification('删除成功');
        }
      }
    },

    // 清空所有激活码记录
    clearAllLicenseRecords() {
      if (confirm('确定要清空所有激活码记录吗？此操作不可恢复。')) {
        localStorage.removeItem('licenseHistory');
        this.oldGeneratedLicense = [];
        this.currentPageNum = 1;
        Utils.showNotification('已清空所有记录');
      }
    }
  }
}

// 启动应用
const app = createApp(App)
app.component('SponsorComponent', SponsorComponent)
app.mount('#app')

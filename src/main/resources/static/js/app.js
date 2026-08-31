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
      licenseHistory: [],
      licenseHistoryConfigKey: [],
      generatedLicense: '',
      currentPageNum: 1,
      pageSize: 10,
      currentRecordTab: 'code', // 当前记录页面的 Tab: 'code' 或 'plugin'
      pluginUpdatePageNum: 1,
      products: [],
      plugins: [],
      pluginsUpdateTime: [],
      productsUpdateTime: [],
      filteredProducts: [],
      filteredPlugins: [],
      lastPluginUpdateTime: "",
      lastProductUpdateTime: "",
      version: "",
      searchQuery: '',
      navItems: [
        { id: 'home', name: '首页', icon: 'fas fa-home' },
        { id: 'usage', name: '说明', icon: 'fas fa-book' },
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
      // 防御性校验：如果还没加载完成或不是数组，直接返回空数组，彻底避免抛错
      if (!Array.isArray(this.licenseHistoryConfigKey)) {
        return [];
      }

      return [...this.licenseHistoryConfigKey].sort((a, b) => {
        return new Date(b.generationTime || 0) - new Date(a.generationTime || 0);
      });
    },

    // 分页后的历史记录
    paginatedLicenseHistory() {
      const start = (this.currentPageNum - 1) * this.pageSize;
      const end = start + this.pageSize;
      return this.sortedLicenseHistory.slice(start, end);
    },

    // 激活码总页数
    licenseTotalPages() {
      return Math.ceil(this.sortedLicenseHistory.length / this.pageSize);
    },

    // 插件更新时间倒序排序
    sortedPluginsUpdateTime() {
      return [...this.pluginsUpdateTime].sort((a, b) => {
        return new Date(b.updateTime) - new Date(a.updateTime);
      });
    },

    // 插件更新时间分页
    paginatedPluginsUpdateTime() {
      const start = (this.pluginUpdatePageNum - 1) * this.pageSize;
      const end = start + this.pageSize;
      return this.sortedPluginsUpdateTime.slice(start, end);
    },

    // 插件更新时间总页数
    pluginUpdateTotalPages() {
      return Math.ceil(this.sortedPluginsUpdateTime.length / this.pageSize);
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
    // 加载激活配置项
    this.loadConfig()
    // 加载产品列表
    this.loadProducts()
    // 加载插件列表
    this.loadPlugins()
    // 加载产品更新时间
    this.loadProductUpdateTime()
    // 加载插件更新时间
    this.loadPluginUpdateTime()
    // 加载支持的最高激活版本
    this.loadCommonVersion()
    // 加载授权历史
    this.loadLicenseHistory()
    // 设置默认过期时间
    this.setDefaultExpiryDate()

    // 加载主题设置
    Utils.loadTheme()
    

    // 监听路由变化
    this.handleHashChange = () => {
      this.currentPage = Utils.getCurrentPage()
      this.searchQuery = ''
      // 重置过滤结果
      this.filteredProducts = [...this.products]
      this.filteredPlugins = [...this.plugins]
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

    // 将实例暴露到 window 对象
    window.app = this
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
    },
    
    // 配置相关
    loadConfig() {
      const config = StorageService.getConfig()
      if (StorageService.isConfigured()) {
        this.config = config
      } else {
        this.showConfigModal = true
      }

      // configKey不存在,但config存在
      if (!StorageService.getConfigKey() && StorageService.isConfigured()){
        StorageService.saveConfigKey(config.licenseName,config.assigneeName);
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

    async loadProductUpdateTime() {
      try {
        this.productsUpdateTime = await ApiService.getProductUpdateTime()
        // 获取数组最后一个元素的更新时间
        if (this.productsUpdateTime && this.productsUpdateTime.length > 0) {
          const lastItem = this.productsUpdateTime[this.productsUpdateTime.length - 1]
          this.lastProductUpdateTime = lastItem.updateTime || ''
        }
      } catch (error) {
        console.error('加载插件更新时间失败:', error)
      }
    },

    async getProductUpdateTime() {
      await this.loadProductUpdateTime()
      Utils.showNotification('更新时间已刷新', 'success')
    },

    async loadCommonVersion() {
      try {
        this.version = await ApiService.getCommonVersion()
      } catch (error) {
        console.error('加载版本失败:', error)
      }
    },

    // 搜索功能
    filterItems(query) {
      const searchTerm = query.toLowerCase().trim().replaceAll(' ','')

      if (this.currentPage === 'products') {
        this.filteredProducts = this.products.filter((product) =>
          product.name.toLowerCase().replaceAll(' ','').includes(searchTerm) ||
          (product.description && product.description.toLowerCase().replaceAll(' ','').includes(searchTerm))
        )
      } else if (this.currentPage === 'plugins') {
        this.filteredPlugins = this.plugins.filter((plugin) =>
          plugin.name.toLowerCase().replaceAll(' ','').includes(searchTerm) ||
          (plugin.description && plugin.description.toLowerCase().replaceAll(' ','').includes(searchTerm))
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

    openLink(object) {
      // 在新标签页打开链接
      if (object.link) {
        window.open(object.link, '_blank')
      }
    },

    setExpiryDate(days) {
      const date = new Date()
      date.setDate(date.getDate() + parseInt(days))
      this.licenseConfig.expiryDate = date.toISOString().split('T')[0]
    },

    // 生成激活码
    async generateLicense() {
      if (!StorageService.getConfigKey()) {
        Utils.showNotification('缺少唯一标识，请使用ctrl+F5强制刷新', 'error')
        return
      }

      this.isGenerating = true
      try {
        const result = await ApiService.generateLicense(
            StorageService.getConfigKey(),
            this.selectedItem.productCode,
            this.config.licenseName,
            this.config.assigneeName,
            this.licenseConfig.expiryDate,
            this.licenseConfig.licenseType,
            this.licenseConfig.userCount,
            this.selectedItem.name
        )
        this.generatedLicense = result.activationCode
        
        this.showLicenseModal = false
        this.showResultModal = true
      } catch (error) {
        Utils.showNotification('生成激活码失败，请重试', 'error')
      } finally {
        this.isGenerating = false
        // 更新当前显示的数据
        this.getLicenseHistoryConfigKey();
      }
    },

    async viewPowerConf() {
      try {
        this.powerConfContent = await ApiService.getPowerConf()
        this.showPowerConfModal = true
      } catch (error) {
        console.error('获取 power.conf 失败:', error)
        Utils.showNotification('获取配置文件失败', 'error')
      }
    },

    // 加载历史记录（用于记录页面）
    async loadLicenseHistory() {
      await this.getLicenseHistoryConfigKey();
      this.currentPageNum = 1; // 重置到第一页
    },
    async getLicenseHistoryConfigKey(){
      try {
        // 1. 必须加 await 等待异步网络请求返回结果
        const res = await ApiService.getLicenseHistoryConfigKey();

        // 2. 兼容 axios 各种返回格式 (例如 res 或者是 res.data)
        const data = res && res.data !== undefined ? res.data : res;

        // 3. 校验数据格式，确保赋给 licenseHistoryConfigKey 的一定是数组
        if (Array.isArray(data)) {
          this.licenseHistoryConfigKey = data;
        } else {
          this.licenseHistoryConfigKey = [];
        }
      } catch (error) {
        console.error('加载历史记录失败:', error);
        this.licenseHistoryConfigKey = [];
      }
    },
    async getLicenseHistory(){
      try {
        // 1. 必须加 await 等待异步网络请求返回结果
        this.licenseHistory = await ApiService.getLicenseHistory();
      } catch (error) {
        console.error('加载历史记录失败:', error);
        this.licenseHistory = [];
      }
    },
    // 跳转到指定页
    goToPage(page) {
      if (page >= 1 && page <= this.licenseTotalPages) {
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
      if (this.currentPageNum < this.licenseTotalPages) {
        this.currentPageNum++;
      }
    },

    // 插件更新时间分页
    goToPluginUpdatePage(page) {
      if (page >= 1 && page <= this.pluginUpdateTotalPages) {
        this.pluginUpdatePageNum = page;
      }
    },
    prevPluginUpdatePage() {
      if (this.pluginUpdatePageNum > 1) {
        this.pluginUpdatePageNum--;
      }
    },
    nextPluginUpdatePage() {
      if (this.pluginUpdatePageNum < this.pluginUpdateTotalPages) {
        this.pluginUpdatePageNum++;
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

    // 获取许可证类型文本
    getLicenseTypeText(value) {
      return Utils.getLicenseTypeText(value);
    },

    // 删除单条激活码记录
    async deleteLicenseRecord(delKey) {
      if (!confirm('确定要删除这条激活码记录吗？')) {
        return
      }

      try {
        // ✅ 等待删除完成
        await this.delLicenseHistory(delKey);

        // ✅ 删除成功后刷新数据
        await this.getLicenseHistoryConfigKey();

        // 如果当前页没有数据了，跳转到上一页
        if (this.paginatedLicenseHistory.length === 0 && this.currentPageNum > 1) {
          this.currentPageNum--;
        }
        Utils.showNotification('删除成功');
      } catch (error) {
        Utils.showNotification('删除失败，请重试', 'error');
      }
    },

    // 清空所有激活码记录
    clearAllLicenseRecords() {
      if (confirm('确定要清空所有激活码记录吗？此操作不可恢复。')) {
        // 删除缓存中的记录
        this.delLicenseHistory('all')
        this.licenseHistoryConfigKey = [];
        this.currentPageNum = 1;
        Utils.showNotification('已清空所有记录');
      }
    },
    async delLicenseHistory(delKey) {
      try {
        await ApiService.delLicenseHistory(delKey);
      } catch (error) {
        console.error('删除 API 调用失败:', error);
        throw error; // 抛出错误让上层处理
      }
    }
  }
}

// 启动应用
const app = createApp(App)
app.component('SponsorComponent', SponsorComponent)
app.mount('#app')

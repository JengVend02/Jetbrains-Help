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
      isGenerating: false,
      config: {
        licenseName: '',
        assigneeName: ''
      },
      licenseConfig: {
        expiryDate: ''
      },
      selectedItem: null,
      generatedLicense: '',
      products: [],
      plugins: [],
      filteredProducts: [],
      filteredPlugins: [],
      searchQuery: '',
      navItems: [
        { id: 'home', name: '首页', icon: 'fas fa-home' },
        { id: 'products', name: '产品', icon: 'fas fa-cube' },
        { id: 'plugins', name: '插件', icon: 'fas fa-puzzle-piece' },
        { id: 'jrebel', name: 'JRebel', icon: 'fas fa-fire' },
        { id: 'sponsor', name: '赞助', icon: 'fas fa-heart' }
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
    }
  },

  mounted() {
    this.loadConfig()
    this.loadProducts()
    this.loadPlugins()
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
  },

  methods: {
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

    // 生成激活码
    async generateLicense() {
      this.isGenerating = true

      try {
        const result = await ApiService.generateLicense(this.selectedItem.productCode, this.config.licenseName, this.config.assigneeName, this.licenseConfig.expiryDate)
        this.generatedLicense = result
        this.showLicenseModal = false
        this.showResultModal = true
      } catch (error) {
        console.error('生成激活码失败:', error)
        Utils.showNotification('生成激活码失败，请重试', 'error')
      } finally {
        this.isGenerating = false
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
    getProductIcon(product) {
      if (product.iconClass && product.iconClass.startsWith('icon-')) {
        const iconName = product.iconClass.replace('icon-', '');

        // 图标映射表
        const iconMap = {
          'ii': 'https://resources.jetbrains.com/storage/logos/web/intellij-idea/intellij-idea.svg',
          'ps': 'https://resources.jetbrains.com/storage/logos/web/phpstorm/phpstorm.svg',
          'ac': 'https://resources.jetbrains.com/storage/logos/web/appcode/appcode.svg',
          'db': 'https://resources.jetbrains.com/storage/logos/web/datagrip/datagrip.svg',
          'rm': 'https://resources.jetbrains.com/storage/logos/web/rubymine/rubymine.svg',
          'ws': 'https://resources.jetbrains.com/storage/logos/web/webstorm/webstorm.svg',
          'rd': 'https://resources.jetbrains.com/storage/logos/web/rider/rider.svg',
          'cl': 'https://resources.jetbrains.com/storage/logos/web/clion/clion.svg',
          'pc': 'https://resources.jetbrains.com/storage/logos/web/pycharm/pycharm.svg',
          'go': 'https://resources.jetbrains.com/storage/logos/web/goland/goland.svg',
          'ds': 'https://resources.jetbrains.com/storage/logos/web/dataspell/dataspell.svg',
          'dc': 'https://resources.jetbrains.com/storage/logos/web/dotcover/dotcover.svg',
          'dpn': 'https://resources.jetbrains.com/storage/logos/web/dottrace/dottrace.svg',
          'dm': 'https://resources.jetbrains.com/storage/logos/web/dotmemory/dotmemory.svg',
          'rr': 'https://resources.jetbrains.com/storage/logos/web/rustrover/rustrover.svg',
          'qa': 'https://resources.jetbrains.com/storage/logos/web/aqua/aqua.svg',
          'al': 'https://resources.jetbrains.com/storage/logos/web/toolbox/toolbox.svg'
        };

        return iconMap[iconName] || '/images/plugin.svg';
      }
      return '/images/plugin.svg';
    },

    getPluginIcon(plugin) {
      return plugin.icon || '/images/plugin.svg'
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
    }
  }
}

// 启动应用
const app = createApp(App)
app.component('SponsorComponent', SponsorComponent)
app.mount('#app')

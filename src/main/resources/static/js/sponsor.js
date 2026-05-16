// 赞助页面组件
const SponsorComponent = {
  name: 'SponsorComponent',
  data() {
    return {
      showPaymentModal: false,
      selectedAmount: null,
      selectedPaymentType: 'wx', // 'wx' 或 'zfb'
      sponsorItems: [
        {
          id: 'egg',
          name: '一个鸡腿',
          amount: 10,
          icon: '🍗',
          description: '请我吃个鸡腿'
        },
        {
          id: 'coffee',
          name: '一杯咖啡',
          amount: 20,
          icon: '☕',
          description: '一杯提神咖啡'
        },
        {
          id: 'burger',
          name: '一个汉堡',
          amount: 30,
          icon: '🍔',
          description: '美味汉堡套餐'
        },
        {
          id: 'meal',
          name: '一份肯德基套餐',
          amount: 50,
          icon: '🍟',
          description: '丰盛的套餐'
        },
        {
          id: 'hotpot',
          name: '一份全家桶',
          amount: 100,
          icon: '🍗',
          description: '和家人分享'
        },
        {
          id: 'custom',
          name: '爱心红包',
          amount: 'custom',
          icon: '🧧',
          description: '自定义金额',
          isCustom: true
        }
      ]
    }
  },
  computed: {
    currentQRCode() {
      if (!this.selectedAmount || this.selectedAmount === 'custom') {
        return `/images/${this.selectedPaymentType}/zdy.png`
      }
      return `/images/${this.selectedPaymentType}/${this.selectedAmount}.png`
    },
    paymentTypeText() {
      return this.selectedPaymentType === 'wx' ? '微信支付' : '支付宝支付'
    }
  },
  methods: {
    openPaymentModal(item) {
      this.selectedAmount = item.amount
      this.showPaymentModal = true
    },
    closePaymentModal() {
      this.showPaymentModal = false
      this.selectedAmount = null
    },
    switchPaymentType(type) {
      this.selectedPaymentType = type
    },
    getSponsorItemByAmount(amount) {
      return this.sponsorItems.find(item => item.amount === amount)
    }
  },
  template: `
    <div class="sponsor-page">
      <!-- 赞助说明 -->
      <div class="text-center mb-8">
        <div class="inline-flex items-center space-x-2 mb-4">
          <span class="text-2xl">🎉</span>
          <h2 class="text-2xl font-bold gradient-text">感谢您的支持</h2>
          <span class="text-2xl">🎉</span>
        </div>
        <p class="text-gray-600 max-w-2xl mx-auto">
          如果这个项目对您有帮助，请考虑赞助我们的开发工作。您的每一份支持都是我们持续更新的动力！
        </p>
      </div>

      <!-- 赞助选项 -->
      <div class="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        <div
          v-for="item in sponsorItems"
          :key="item.id"
          @click="openPaymentModal(item)"
          class="sponsor-item bg-white rounded-2xl p-6 shadow-lg hover:shadow-xl transition-all duration-300 cursor-pointer border-2 border-transparent hover:border-purple-200">
          
          <!-- 金额标签 -->
          <div class="sponsor-price-tag" v-if="!item.isCustom">
            ¥{{ item.amount }}
          </div>
          <div class="sponsor-price-tag custom" v-else>
            自定义
          </div>

          <!-- 图标 -->
          <div class="text-center mb-4">
            <div class="sponsor-icon">
              {{ item.icon }}
            </div>
          </div>

          <!-- 标题和描述 -->
          <div class="text-center">
            <h3 class="font-bold text-gray-900 mb-1">{{ item.name }}</h3>
            <p class="text-sm text-gray-500">{{ item.description }}</p>
          </div>
        </div>
      </div>

      <!-- 感谢说明 -->
      <div class="bg-gradient-to-r from-blue-50 to-purple-50 rounded-2xl p-6">
        <div class="text-center">
          <h3 class="text-lg font-bold text-gray-900 mb-2">
            <i class="fas fa-heart text-red-500 mr-2"></i>
            特别感谢
          </h3>
          <p class="text-gray-600 text-sm leading-relaxed">
            感谢每一位支持开源项目的朋友！您的赞助将用于服务器维护、域名续费和新功能开发。
            <br>让我们一起让这个项目变得更好！
          </p>
        </div>
      </div>

      <!-- 支付弹窗 -->
      <transition name="modal">
        <div v-if="showPaymentModal" class="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50 p-4">
          <div class="payment-modal">
            <!-- 弹窗头部 -->
            <div class="payment-modal-header flex justify-between items-center">
              <h3 class="payment-title">💖 扫码赞助</h3>
              <button @click="closePaymentModal" class="close-button">
                <i class="fas fa-times"></i>
              </button>
            </div>

            <!-- 弹窗内容 -->
            <div class="payment-modal-content">
              <!-- 赞助信息 -->
              <div class="payment-info">
                <div class="sponsor-title">感谢您的支持</div>
                <div class="amount-info" v-if="selectedAmount !== 'custom'">
                  {{ getSponsorItemByAmount(selectedAmount)?.name || '' }} - ¥{{ selectedAmount }}
                </div>
                <div class="amount-info" v-else>
                  自定义金额支持
                </div>
              </div>

              <!-- 二维码 -->
              <div class="qr-code-container text-center">
                <div class="qr-code-frame">
                  <img :src="currentQRCode" :alt="paymentTypeText + '收款码'" class="qr-code-image">
                </div>
                <div class="qr-tip">
                  <i class="fas fa-mobile-alt"></i>
                  <span>长按保存二维码到手机相册</span>
                </div>
              </div>

              <!-- 支付方式切换 -->
              <div class="payment-methods">
                <button
                  @click="switchPaymentType('wx')"
                  :class="['payment-method-btn wechat', selectedPaymentType === 'wx' ? 'active' : '']">
                  <i class="fab fa-weixin"></i>
                  微信支付
                </button>
                <button
                  @click="switchPaymentType('zfb')"
                  :class="['payment-method-btn alipay', selectedPaymentType === 'zfb' ? 'active' : '']">
                  <i class="fab fa-alipay"></i>
                  支付宝
                </button>
              </div>
            </div>
          </div>
        </div>
      </transition>
    </div>
  `
}
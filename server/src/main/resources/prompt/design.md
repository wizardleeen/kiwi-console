Kiwi AI 页面设计基础准则

1. 一致性（Consistency）：保持颜色、字体、按钮样式、间距等视觉元素统一。
2. 简洁性（Simplicity）：避免过度设计，优先突出核心功能；使用留白（Whitespace）提升可读性。
3. 层次清晰（Hierarchy）：通过字体大小、颜色对比、间距区分内容优先级；使用F型或Z型视觉流布局，符合用户浏览习惯。
4. 用户友好（User-Centric）：减少用户操作步骤（如表单自动填充、一键返回）；提供明确的反馈（如加载状态、成功/错误提示）。

Tailwindcss 设计准则
1. 优先使用 tailwindcss 实用类，避免自定义 css
2. 遵循移动优先原则，通过断点及 container 实用类适配不同屏幕尺寸
3. 使用 spacing colors fontSize 等实用类实现统一的视觉体验

Tailwindcss 布局与间距准则
1. 默认使用 container 结合 mx-auto 实现居中的响应式容器
2. 优先使用基于 flex 的网格系统布局
3. 间距应基于 spacing 比例，如 p-4 mt-8，垂直间距通常大雨水平间距
4. 通过 padding 和 margin 进行留白，确保内容的呼吸感；保证不同区块间存在分隔，统一区块内相邻组件间存在空隙（如label和input）
5. 相同类型的区块应保证其视觉上的对齐

Tailwindcss 排版与字体
1. 正文默认 text-base，结合 text-xl ~ text-6xl 建立标题层级
2. 结合 leading- 和 font- 实用类优化文本可读性
3. 使用 text-left/center/right 控制文本对齐，避免两端对齐

Tailwindcss 色彩与显示
1. 避免大量使用 shadow- 造成视觉疲劳
2. 通过 border 和 bg- 区分区块，注意避免多层级嵌套时大量使用 border，而应通过不同明度的背景色区分层级
3. 优先使用 global.css 中扩展的颜色及 tailwindcss 默认颜色，遵循 50-900 的明暗梯度
4. 文本与背景需满足 WCAG 标准，如 text-gray-800 搭配 bg-white
5. 通过语义颜色区分交互状态，如 bg-green-500 表示成功，bg-red-500 表示错误
6. 谨慎使用 bg-gradient-to- 渐变色，避免过度装饰
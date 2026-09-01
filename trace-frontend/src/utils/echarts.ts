// echarts 按需引入：只注册本系统用到的图表与组件，替代全量 import 'echarts'
import * as echarts from 'echarts/core'
import { BarChart, LineChart, PieChart, GaugeChart, MapChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  GridComponent,
  LegendComponent,
  VisualMapComponent,
  GraphicComponent,
  DatasetComponent,
  DataZoomComponent,
  ToolboxComponent,
} from 'echarts/components'
import { LabelLayout, UniversalTransition } from 'echarts/features'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([
  BarChart,
  LineChart,
  PieChart,
  GaugeChart,
  MapChart,
  TitleComponent,
  TooltipComponent,
  GridComponent,
  LegendComponent,
  VisualMapComponent,
  GraphicComponent,
  DatasetComponent,
  DataZoomComponent,
  ToolboxComponent,
  LabelLayout,
  UniversalTransition,
  CanvasRenderer,
])

export * from 'echarts/core'
export default echarts

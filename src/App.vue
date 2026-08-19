<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app'

onLaunch(() => undefined)
</script>

<style>
html,
body,
#app {
  min-height: 100%;
  margin: 0;
  background: #f8f8fb;
}

page {
  min-height: 100%;
  background: #f8f8fb;
  color: #18203a;
  font-family: -apple-system, BlinkMacSystemFont, "PingFang SC", "Microsoft YaHei", sans-serif;
}

view,
text,
button,
input {
  box-sizing: border-box;
}

button::after {
  border: 0;
}
</style>

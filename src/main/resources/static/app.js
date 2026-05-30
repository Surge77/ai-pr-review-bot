const status = document.getElementById('status');
const feed = document.getElementById('feed');
const progress = document.getElementById('progress');

const client = new StompJs.Client({
  webSocketFactory: () => new SockJS('/ws'),
  reconnectDelay: 2000,
  onConnect: () => {
    status.textContent = 'connected';
    status.className = 'up';
    client.subscribe('/topic/progress', (msg) => render(JSON.parse(msg.body)));
  },
  onWebSocketClose: () => {
    status.textContent = 'disconnected — retrying…';
    status.className = 'down';
  },
});

function render(e) {
  if (e.filesTotal > 0) {
    progress.style.width = Math.round((e.filesDone / e.filesTotal) * 100) + '%';
  }
  const li = document.createElement('li');
  li.className = e.stage;
  const when = new Date(e.timestamp).toLocaleTimeString();
  li.innerHTML = `<span class="stage">${e.stage}</span>`
    + `${e.repoFullName}#${e.prNumber} — ${e.message}`
    + `<time>${when}</time>`;
  feed.prepend(li);
}

client.activate();

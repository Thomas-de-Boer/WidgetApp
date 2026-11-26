// Theme switch, release info and the three live widgets in the hero
// (maze, generated art, Plinko). No framework.

(function () {
  "use strict";

  var root = document.documentElement;
  var reduceMotion = window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches;

  function remember(key, value) {
    try { localStorage.setItem(key, value); } catch (e) { /* private mode: just don't persist */ }
  }

  // ---------------------------------------------------------------- theme

  var themeToggle = document.getElementById("theme-toggle");
  function isDark() { return root.classList.contains("t-dark"); }
  function syncTheme() {
    themeToggle.setAttribute("aria-checked", String(isDark()));
    document.querySelector('meta[name="theme-color"]').content = isDark() ? "#151515" : "#faf9f6";
  }
  themeToggle.addEventListener("click", function () {
    var next = isDark() ? "light" : "dark";
    root.className = "t-" + next;
    remember("widgets-theme", next);
    syncTheme();
  });
  syncTheme();

  // -------------------------------------------------------- release info
  // The download button always works (GitHub redirects /latest/download/ to
  // the newest release). This only fills in the version line under it.

  var info = document.getElementById("release-info");
  if (window.fetch) {
    fetch("https://api.github.com/repos/Thomas-de-Boer/WidgetApp/releases/latest")
      .then(function (r) { return r.ok ? r.json() : null; })
      .then(function (release) {
        if (!release || !release.tag_name) return;
        var apk = (release.assets || []).filter(function (a) { return a.name === "app-release.apk"; })[0];
        var version = release.tag_name.replace(/^v/, "");
        var date = new Date(release.published_at).toLocaleDateString("en-GB", { day: "numeric", month: "long", year: "numeric" });
        var size = apk ? " (" + Math.round(apk.size / 1048576) + " MB)" : "";
        info.textContent = "Version " + version + size + ", released " + date + ". Needs Android 8.0 or newer.";
      })
      .catch(function () { /* keep the static line */ });
  }

  // Small helpers.
  function rand(n) { return Math.floor(Math.random() * n); }
  function shuffle(a) {
    for (var i = a.length - 1; i > 0; i--) { var j = rand(i + 1); var t = a[i]; a[i] = a[j]; a[j] = t; }
    return a;
  }
  // The widget colours are fixed (they don't follow the page theme), so read them once.
  var colours = {};
  function css(name) {
    if (!(name in colours)) colours[name] = getComputedStyle(root).getPropertyValue(name).trim();
    return colours[name];
  }

  // ----------------------------------------------------------------- maze
  // Depth-first maze with one way out, solved with a breadth-first search.
  // The route is drawn once when the maze scrolls into view; tap for a new one.

  (function () {
    var button = document.getElementById("maze");
    var canvas = button.querySelector("canvas");
    var ctx = canvas.getContext("2d");
    var N = 9, S = canvas.width, M = 24, cell = (S - 2 * M) / N;
    var walls, route, timer = null;

    function build() {
      // walls[i] = [top, right, bottom, left]
      walls = [];
      for (var i = 0; i < N * N; i++) walls.push([true, true, true, true]);
      var seen = new Array(N * N).fill(false), stack = [0];
      seen[0] = true;
      while (stack.length) {
        var c = stack[stack.length - 1], x = c % N, y = (c / N) | 0;
        var options = shuffle([[0, -1, 0, 2], [1, 0, 1, 3], [0, 1, 2, 0], [-1, 0, 3, 1]]).filter(function (d) {
          var nx = x + d[0], ny = y + d[1];
          return nx >= 0 && ny >= 0 && nx < N && ny < N && !seen[ny * N + nx];
        });
        if (!options.length) { stack.pop(); continue; }
        var d = options[0], n = (y + d[1]) * N + x + d[0];
        walls[c][d[2]] = false;
        walls[n][d[3]] = false;
        seen[n] = true;
        stack.push(n);
      }
      // Breadth-first search from the top-left cell to the bottom-right one.
      var prev = new Array(N * N).fill(-1), queue = [0], end = N * N - 1;
      prev[0] = 0;
      while (queue.length) {
        var c2 = queue.shift(), x2 = c2 % N, y2 = (c2 / N) | 0;
        [[0, -1], [1, 0], [0, 1], [-1, 0]].forEach(function (d, side) {
          var nx = x2 + d[0], ny = y2 + d[1], n2 = ny * N + nx;
          if (!walls[c2][side] && prev[n2] === -1) { prev[n2] = c2; queue.push(n2); }
        });
      }
      route = [];
      for (var at = end; at !== 0; at = prev[at]) route.unshift(at);
      route.unshift(0);
    }

    function centre(c) { return [M + (c % N + 0.5) * cell, M + (((c / N) | 0) + 0.5) * cell]; }

    function draw(steps) {
      ctx.fillStyle = css("--w-bg");
      ctx.fillRect(0, 0, S, S);
      ctx.lineCap = "round";
      ctx.lineJoin = "round";

      // start and exit
      var a = centre(0), b = centre(N * N - 1), r = cell * 0.22;
      ctx.fillStyle = css("--w-plus");
      ctx.beginPath(); ctx.arc(a[0], a[1], r, 0, 7); ctx.fill();
      ctx.fillStyle = css("--w-minus");
      ctx.beginPath(); ctx.arc(b[0], b[1], r, 0, 7); ctx.fill();

      if (steps > 1) {
        ctx.strokeStyle = css("--w-path");
        ctx.lineWidth = cell * 0.26;
        ctx.beginPath();
        for (var i = 0; i < steps && i < route.length; i++) {
          var p = centre(route[i]);
          if (i === 0) ctx.moveTo(p[0], p[1]); else ctx.lineTo(p[0], p[1]);
        }
        ctx.stroke();
      }

      ctx.strokeStyle = css("--w-ink");
      ctx.lineWidth = 6;
      ctx.beginPath();
      for (var c = 0; c < N * N; c++) {
        var x = M + (c % N) * cell, y = M + ((c / N) | 0) * cell;
        if (walls[c][0] && c !== 0) { ctx.moveTo(x, y); ctx.lineTo(x + cell, y); }
        if (walls[c][3]) { ctx.moveTo(x, y); ctx.lineTo(x, y + cell); }
        if (c % N === N - 1 && walls[c][1]) { ctx.moveTo(x + cell, y); ctx.lineTo(x + cell, y + cell); }
        if (((c / N) | 0) === N - 1 && walls[c][2] && c !== N * N - 1) { ctx.moveTo(x, y + cell); ctx.lineTo(x + cell, y + cell); }
      }
      ctx.stroke();
    }

    function solve() {
      clearInterval(timer);
      if (reduceMotion) { draw(route.length); return; }
      var steps = 1;
      timer = setInterval(function () {
        steps++;
        draw(steps);
        if (steps >= route.length) clearInterval(timer);
      }, 40); // the widget's own flip interval
    }

    build();
    draw(0);

    // The one animation that plays by itself: the route, once it's on screen.
    if ("IntersectionObserver" in window) {
      var io = new IntersectionObserver(function (entries) {
        if (entries[0].isIntersecting) { io.disconnect(); setTimeout(solve, 500); }
      }, { threshold: 0.6 });
      io.observe(canvas);
    } else {
      solve();
    }

    button.addEventListener("click", function () {
      clearInterval(timer);
      build();
      draw(0);
      setTimeout(solve, 250);
    });
  })();

  // ------------------------------------------------------- generated art
  // Smoothed value noise mapped onto a random three-colour ramp, drawn small
  // and scaled up by the browser.

  (function () {
    var button = document.getElementById("gen");
    var canvas = button.querySelector("canvas");
    var ctx = canvas.getContext("2d");
    var W = canvas.width, H = canvas.height;
    var ramps = [
      ["#10141F", "#4FC3F7", "#DCE3F2"],
      ["#2B1433", "#FF6F91", "#FFD166"],
      ["#16261D", "#7BD389", "#FFC857"],
      ["#23255F", "#9B5DE5", "#FFC3A0"],
      ["#10141F", "#EF5350", "#FFC857"]
    ];
    var last = -1;

    function hex(h) { return [1, 3, 5].map(function (i) { return parseInt(h.substr(i, 2), 16); }); }
    function smooth(t) { return t * t * (3 - 2 * t); }

    function draw() {
      var pick;
      do { pick = rand(ramps.length); } while (pick === last);
      last = pick;
      var ramp = ramps[pick].map(hex);

      var G = 6, grid = [];
      for (var i = 0; i < (G + 1) * (G * 2 + 1); i++) grid.push(Math.random());
      function value(x, y) {
        var gx = x / W * G, gy = y / H * G * 2;
        var x0 = Math.floor(gx), y0 = Math.floor(gy), tx = smooth(gx - x0), ty = smooth(gy - y0);
        function g(a, b) { return grid[b * (G + 1) + a]; }
        var top = g(x0, y0) + (g(x0 + 1, y0) - g(x0, y0)) * tx;
        var bottom = g(x0, y0 + 1) + (g(x0 + 1, y0 + 1) - g(x0, y0 + 1)) * tx;
        return top + (bottom - top) * ty;
      }

      var img = ctx.createImageData(W, H);
      for (var y = 0; y < H; y++) {
        for (var x = 0; x < W; x++) {
          var v = value(x * 0.999, y * 0.999) * 0.7 + value((x * 2.3) % W, (y * 2.3) % H) * 0.3;
          var t = Math.min(1, Math.max(0, (v - 0.2) / 0.6)) * 2;
          var a = ramp[t < 1 ? 0 : 1], b = ramp[t < 1 ? 1 : 2], f = t < 1 ? t : t - 1;
          var o = (y * W + x) * 4;
          img.data[o] = a[0] + (b[0] - a[0]) * f;
          img.data[o + 1] = a[1] + (b[1] - a[1]) * f;
          img.data[o + 2] = a[2] + (b[2] - a[2]) * f;
          img.data[o + 3] = 255;
        }
      }
      ctx.putImageData(img, 0, 0);
    }

    draw();
    button.addEventListener("click", draw);
  })();

  // ---------------------------------------------------------------- plinko
  // A small version of the widget: tap to drop a ball. A ball in a + slot
  // scores that many points and comes back as that many new balls; the
  // middle slot (−1) takes it away. Chains stop growing at 40 balls.

  (function () {
    var button = document.getElementById("plinko");
    var canvas = button.querySelector("canvas");
    var ctx = canvas.getContext("2d");
    var W = canvas.width, H = canvas.height;
    var SLOTS = [4, 3, 2, 1, -1, 1, 2, 3, 4];
    var s = W / 9, ROWS = 8, TOP = 92, DY = 36, PIN_R = 5, BALL_R = 11;
    var SLOT_Y = TOP + ROWS * DY, STEP_MS = 95, MAX_CHAIN = 40;
    var balls = [], flashes = [], points = 0, chain = 0, running = false;

    function pinX(row, k) { return row % 2 === 0 ? (k + 0.5) * s : k * s; }

    function newBall(delay) {
      var moves = [];
      for (var r = 0; r < ROWS; r++) moves.push(Math.random() < 0.5 ? -1 : 1);
      balls.push({ moves: moves, start: performance.now() + delay });
    }

    // Position of a ball t milliseconds after it starts falling.
    function where(ball, t) {
      var x = 4.5 * s, y0 = TOP - 46;
      if (t < 0) return null;
      var step = Math.floor(t / STEP_MS), f = (t % STEP_MS) / STEP_MS;
      // falling from the drop point onto the first pin
      if (step === 0) return [x, y0 + (TOP - BALL_R - PIN_R - y0) * f * f];
      var row = step - 1;
      for (var i = 0; i < row && i < ROWS; i++) x += ball.moves[i] * s / 2;
      if (row >= ROWS) return { landed: Math.round(x / s - 0.5) };
      var nx = x + ball.moves[row] * s / 2;
      var ya = TOP + row * DY - BALL_R - PIN_R, yb = ya + DY;
      // a small hop off the pin, then down to the next one
      return [x + (nx - x) * f, ya + (yb - ya) * f * f - Math.sin(f * Math.PI) * 6];
    }

    function frame(now) {
      ctx.fillStyle = css("--w-bg");
      ctx.fillRect(0, 0, W, H);

      // score line
      ctx.fillStyle = css("--w-ink");
      ctx.font = "600 26px system-ui, sans-serif";
      ctx.textBaseline = "middle";
      ctx.textAlign = "left";
      ctx.fillText(points + (points === 1 ? " point" : " points"), 22, 30);
      ctx.textAlign = "right";
      ctx.fillStyle = chain ? css("--w-ball") : css("--w-pin");
      ctx.fillText(chain ? "chain " + chain : "tap to drop", W - 22, 30);

      // pins
      ctx.fillStyle = css("--w-pin");
      for (var r = 0; r < ROWS; r++) {
        for (var k = 0; k < 10; k++) {
          var px = pinX(r, k);
          if (px <= 4 || px >= W - 4) continue;
          ctx.beginPath(); ctx.arc(px, TOP + r * DY, PIN_R, 0, 7); ctx.fill();
        }
      }

      // slots
      ctx.textAlign = "center";
      ctx.font = "700 24px system-ui, sans-serif";
      SLOTS.forEach(function (v, i) {
        var lit = flashes.some(function (fl) { return fl.slot === i && now - fl.at < 260; });
        ctx.globalAlpha = lit ? 1 : 0.28;
        ctx.fillStyle = v < 0 ? css("--w-minus") : css("--w-plus");
        roundRect(i * s + 3, SLOT_Y + 8, s - 6, H - SLOT_Y - 16, 8);
        ctx.globalAlpha = 1;
        ctx.fillStyle = lit ? css("--w-bg") : css("--w-ink");
        ctx.fillText((v > 0 ? "+" : "−") + Math.abs(v), (i + 0.5) * s, SLOT_Y + 8 + (H - SLOT_Y - 16) / 2);
      });

      // balls
      ctx.fillStyle = css("--w-ball");
      balls = balls.filter(function (ball) {
        var p = where(ball, now - ball.start);
        if (p === null) return true;
        if (p.landed !== undefined) { land(p.landed, now); return false; }
        ctx.beginPath(); ctx.arc(p[0], p[1], BALL_R, 0, 7); ctx.fill();
        return true;
      });
      flashes = flashes.filter(function (fl) { return now - fl.at < 260; });

      if (balls.length || flashes.length) {
        requestAnimationFrame(frame);
      } else {
        running = false;
        // Leave the chain count up for a moment, then clear it once.
        if (chain) {
          chain = 0;
          setTimeout(function () { if (!running) frame(performance.now()); }, 1200);
        }
      }
    }

    function roundRect(x, y, w, h, r) {
      ctx.beginPath();
      ctx.moveTo(x + r, y);
      ctx.arcTo(x + w, y, x + w, y + h, r);
      ctx.arcTo(x + w, y + h, x, y + h, r);
      ctx.arcTo(x, y + h, x, y, r);
      ctx.arcTo(x, y, x + w, y, r);
      ctx.fill();
    }

    function land(slot, now) {
      var v = SLOTS[slot];
      flashes.push({ slot: slot, at: now });
      if (v < 0) return;
      points += v;
      for (var i = 0; i < v && chain < MAX_CHAIN; i++) {
        chain++;
        newBall(120 + i * 160);
      }
    }

    function start() {
      if (running) return;
      running = true;
      requestAnimationFrame(frame);
    }

    button.addEventListener("click", function () {
      chain++;
      newBall(0);
      start();
    });

    frame(performance.now()); // the empty board
  })();
})();

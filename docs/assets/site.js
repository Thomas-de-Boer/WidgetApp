// Theme switch, release info, and the widgets on the page. The widgets are
// ports of the app's own drawing code (PlinkoBoard/PlinkoRenderer,
// MazeRenderer, NoiseRenderer), with the same colours and sizes.

(function () {
  "use strict";

  var root = document.documentElement;
  var reduceMotion = window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches;

  function remember(key, value) {
    try { localStorage.setItem(key, value); } catch (e) { /* private mode: just don't persist */ }
  }
  function recall(key) {
    try { return localStorage.getItem(key); } catch (e) { return null; }
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

  // ------------------------------------------------------------- helpers

  function hex(c) { return "#" + (c & 0xffffff).toString(16).padStart(6, "0"); }
  function blend(from, to, t) {
    t = Math.max(0, Math.min(1, t));
    var out = 0;
    for (var shift = 16; shift >= 0; shift -= 8) {
      var a = (from >> shift) & 255, b = (to >> shift) & 255;
      out |= Math.round(a + (b - a) * t) << shift;
    }
    return out;
  }
  function popcount(n) { var c = 0; while (n) { c += n & 1; n >>>= 1; } return c; }

  // Runs fn while the element is on screen and the tab is visible.
  function whileVisible(el, start, stop) {
    var onScreen = false;
    function update() { if (onScreen && !document.hidden) start(); else stop(); }
    if ("IntersectionObserver" in window) {
      new IntersectionObserver(function (entries) { onScreen = entries[0].isIntersecting; update(); }).observe(el);
    } else {
      onScreen = true;
    }
    document.addEventListener("visibilitychange", update);
    update();
  }

  // -------------------------------------------------------- release info
  // The download button always works: GitHub sends /releases/latest/download/
  // to the newest release. This fills in the version line and the notes.

  var info = document.getElementById("release-info");
  if (window.fetch) {
    fetch("https://api.github.com/repos/Thomas-de-Boer/WidgetApp/releases/latest")
      .then(function (r) { return r.ok ? r.json() : null; })
      .then(function (release) {
        if (!release || !release.tag_name) return;
        var apk = (release.assets || []).filter(function (a) { return a.name === "app-release.apk"; })[0];
        var version = release.tag_name.replace(/^v/, "");
        var date = new Date(release.published_at).toLocaleDateString("en-GB", { day: "numeric", month: "long", year: "numeric" });
        var size = apk ? ", " + Math.round(apk.size / 1048576) + " MB" : "";
        info.textContent = "Version " + version + size + ", from " + date + ". Needs Android 8.0 or newer.";
        showChanges(version, release.body || "");
      })
      .catch(function () { /* keep the static line */ });
  }

  // The "What's new" part of the release notes. They're written in a small
  // part of Markdown: headings, nested lists and bold, which is all this reads.
  function showChanges(version, body) {
    var part = body.split(/^## /m).filter(function (s) { return /^What.s new/i.test(s); })[0];
    if (!part) return;
    var target = document.getElementById("changes-body");
    var stack = [{ indent: -1, el: target }];

    function inline(el, text) {
      text.split(/(\*\*[^*]+\*\*)/).forEach(function (piece) {
        if (/^\*\*.*\*\*$/.test(piece)) {
          var b = document.createElement("strong");
          b.textContent = piece.slice(2, -2);
          el.appendChild(b);
        } else if (piece) {
          el.appendChild(document.createTextNode(piece));
        }
      });
    }

    part.split(/\r?\n/).slice(1).forEach(function (line) {
      var m = line.match(/^(\s*)- (.*)$/);
      if (!m) {
        if (line.trim() && !/^#/.test(line)) {
          var p = document.createElement("p");
          inline(p, line.trim());
          target.appendChild(p);
          stack = [{ indent: -1, el: target }];
        }
        return;
      }
      var indent = m[1].length;
      while (stack.length > 1 && stack[stack.length - 1].indent >= indent) stack.pop();
      var top = stack[stack.length - 1];
      var list = top.list && top.list.parentNode === top.el ? top.list : null;
      if (!list) {
        list = document.createElement("ul");
        top.el.appendChild(list);
        top.list = list;
      }
      var li = document.createElement("li");
      inline(li, m[2]);
      list.appendChild(li);
      stack.push({ indent: indent, el: li });
    });

    document.getElementById("changes-version").textContent = version;
    document.getElementById("changes").hidden = false;
  }

  // ---------------------------------------------------------------- plinko
  // PlinkoBoard and PlinkoRenderer, theme 1. One frame is 40 ms, as on the phone.

  (function () {
    var button = document.getElementById("plinko");
    var canvas = button.querySelector("canvas");
    var ctx = canvas.getContext("2d");

    var W = 640, H = 480;
    var ROWS = 8, SLOTS = 9, SLOT_VALUES = [4, 3, 2, 1, -1, 1, 2, 3, 4];
    var BALLS_PER_LEVEL = 5, MAX_ON_SCREEN = 30, MAX_IN_PLAY = 99, START_LIVES = 3;
    var LEAN = 0.027, BONUS_MIN_ROW = 3;
    var DROP_FRAMES = 8, ROW_FRAMES = 6, FALL_FRAMES = 7;
    var TRAVEL_FRAMES = DROP_FRAMES + (ROWS - 1) * ROW_FRAMES + FALL_FRAMES;
    var RELEASE_GAP = 2, BANNER_FRAMES = 40, POPUP_FRAMES = 16;
    var DROP_Y = -1.8, REST_ABOVE_PIN = 0.35, SLOT_Y = 8.55, BOUNCE = 0.28, DROP_SPREAD = 0.3;
    var UNIT_X = W / 9.5, TOP = 26, SLOT_TOP_Y = 8.05, SLOT_BOTTOM_Y = 9.45;
    var UNIT_Y = (H - 8 - TOP) / (SLOT_BOTTOM_Y - DROP_Y);
    var PIN_RADIUS = 5, BALL_RADIUS = 9, SLOT_GAP = 3, FLASH_FRAMES = 6, BONUS_FLASH_FRAMES = 16;
    var MARGIN = 16, BAR_WIDTH = 150, FRAME_MS = 40;
    // background, pins, ball, text, minus slot, plus slots
    var C = [0x10141F, 0x8C96AD, 0xFFC857, 0xDCE3F2, 0xEF5350, 0x66BB6A];

    var star = new Path2D(), heart = new Path2D();
    for (var p = 0; p < 10; p++) {
      var angle = Math.PI * p / 5 - Math.PI / 2, r = p % 2 === 0 ? 11 : 4.6;
      if (p === 0) star.moveTo(Math.cos(angle) * r, Math.sin(angle) * r);
      else star.lineTo(Math.cos(angle) * r, Math.sin(angle) * r);
    }
    star.closePath();
    heart.moveTo(0, 9);
    heart.bezierCurveTo(-14, -1, -9, -11, 0, -4);
    heart.bezierCurveTo(9, -11, 14, -1, 0, 9);
    heart.closePath();

    var best = Math.max(1, parseInt(recall("widgets-plinko-best"), 10) || 1);
    var level = 1, lives = START_LIVES, score = 0, ballsLeft, phase = "playing", bannerStart = 0;
    var frame = 0, balls = [], waiting = [], popups = [], hits = [], chainSizes = [], lastRelease = -99;
    var bonus;

    function target(lvl) {
      var points = Math.min(1e9, 10 * Math.pow(1.2, lvl - 1));
      return points < 20 ? Math.round(points) : Math.round(points / 5) * 5;
    }
    function multiplier(size) {
      return size >= 100 ? 10 : size >= 60 ? 5 : size >= 30 ? 3 : size >= 15 ? 2 : 1;
    }
    function liveMultiplier() {
      var m = 1;
      balls.concat(waiting).forEach(function (b) { if (!b.spent) m = Math.max(m, multiplier(chainSizes[b.chain])); });
      return m;
    }

    function newLevel() {
      score = 0;
      ballsLeft = BALLS_PER_LEVEL;
      chainSizes = [];
      hits = [];
      var row = BONUS_MIN_ROW + Math.floor(Math.random() * (ROWS - BONUS_MIN_ROW));
      bonus = { row: row, index: Math.floor(Math.random() * (row + 1)), taken: false, at: -99 };
    }
    newLevel();

    // bit n of the path is the direction at pin row n: 0 is left, 1 is right.
    // The board leans a little towards the middle.
    function makePath() {
      var path = 0, rights = 0;
      for (var hop = 0; hop < ROWS; hop++) {
        var side = rights - hop / 2;
        var chanceRight = side < 0 ? 0.5 + LEAN : side > 0 ? 0.5 - LEAN : 0.5;
        if (Math.random() < chanceRight) { path |= 1 << hop; rights++; }
      }
      return path;
    }
    function launch(chain, spent) {
      balls.push({
        start: frame, path: makePath(), jitter: (Math.random() * 2 - 1) * DROP_SPREAD,
        chain: chain, spent: spent, hop: -1
      });
      lastRelease = frame;
    }

    function position(ball) {
      var time = frame - ball.start;
      if (time < DROP_FRAMES) {
        var s0 = time / DROP_FRAMES;
        return [ball.jitter * (1 - s0), DROP_Y + (-REST_ABOVE_PIN - DROP_Y) * s0 * s0];
      }
      time -= DROP_FRAMES;
      var hop = Math.min(Math.floor(time / ROW_FRAMES), ROWS - 1);
      var s = Math.min(1, (time - hop * ROW_FRAMES) / (hop === ROWS - 1 ? FALL_FRAMES : ROW_FRAMES));
      var rights = popcount(ball.path & ((1 << hop) - 1));
      var bit = (ball.path >> hop) & 1;
      var x0 = rights - hop / 2, x1 = rights + bit - (hop + 1) / 2, y0 = hop - REST_ABOVE_PIN;
      if (hop < ROWS - 1) {
        var y1 = hop + 1 - REST_ABOVE_PIN;
        return [x0 + (x1 - x0) * s, y0 + (y1 - y0) * s * s - BOUNCE * 4 * s * (1 - s)];
      }
      return [x0 + (x1 - x0) * Math.min(1, s * 2), y0 + (SLOT_Y - y0) * s * s - BOUNCE * 2 * s * (1 - s)];
    }

    function step() {
      frame++;

      if (waiting.length && balls.length < MAX_ON_SCREEN && frame - lastRelease >= RELEASE_GAP) {
        var next = waiting.shift();
        launch(next.chain, next.spent);
      }

      balls = balls.filter(function (ball) {
        var time = frame - ball.start;
        // every ball rests on one pin per row; any ball can take the glowing one
        var hop = time < DROP_FRAMES ? -1 : Math.min(ROWS - 1, Math.floor((time - DROP_FRAMES) / ROW_FRAMES));
        if (hop > ball.hop) {
          ball.hop = hop;
          if (!bonus.taken && hop === bonus.row && popcount(ball.path & ((1 << hop) - 1)) === bonus.index) {
            bonus.taken = true;
            bonus.at = frame;
            ballsLeft++;
          }
        }
        if (time < TRAVEL_FRAMES) return true;

        var slot = popcount(ball.path);
        var value = SLOT_VALUES[slot];
        hits[slot] = frame;
        if (value > 0) {
          var points = value * multiplier(chainSizes[ball.chain]);
          score += points;
          popups.push({ slot: slot, points: points, start: frame });
          if (!ball.spent) {
            var spent = balls.length + waiting.length + value > MAX_IN_PLAY;
            for (var i = 0; i < value; i++) {
              if (!spent) chainSizes[ball.chain]++;
              waiting.push({ chain: ball.chain, spent: spent });
            }
          }
        }
        return false;
      });
      popups = popups.filter(function (pp) { return frame - pp.start < POPUP_FRAMES; });

      if (phase === "playing" && !balls.length && !waiting.length && ballsLeft === 0) {
        if (score >= target(level)) {
          phase = "won";
        } else {
          lives--;
          phase = lives > 0 ? "lost" : "over";
        }
        bannerStart = frame;
      } else if (phase !== "playing" && frame - bannerStart >= BANNER_FRAMES) {
        if (phase === "won") {
          level++;
          if (level > best) { best = level; remember("widgets-plinko-best", String(best)); }
        } else if (phase === "over") {
          level = 1;
          lives = START_LIVES;
        }
        phase = "playing";
        newLevel();
      }
    }

    function busy() {
      var lastHit = Math.max.apply(null, hits.filter(function (h) { return h !== undefined; }).concat(-99));
      return balls.length || waiting.length || popups.length || phase !== "playing" ||
        frame - lastHit < FLASH_FRAMES || frame - bonus.at < BONUS_FLASH_FRAMES;
    }

    // ---- drawing

    function px(x) { return W / 2 + x * UNIT_X; }
    function py(y) { return TOP + (y - DROP_Y) * UNIT_Y; }
    function fill(c, alpha) { ctx.globalAlpha = alpha === undefined ? 1 : alpha; ctx.fillStyle = hex(c); }
    function roundRect(x0, y0, x1, y1, r) {
      ctx.beginPath();
      ctx.moveTo(x0 + r, y0);
      ctx.arcTo(x1, y0, x1, y1, r);
      ctx.arcTo(x1, y1, x0, y1, r);
      ctx.arcTo(x0, y1, x0, y0, r);
      ctx.arcTo(x0, y0, x1, y0, r);
      ctx.fill();
    }
    function circle(x, y, r) { ctx.beginPath(); ctx.arc(x, y, r, 0, Math.PI * 2); ctx.fill(); }
    function font(size) { ctx.font = "bold " + size + "px Roboto, system-ui, sans-serif"; }

    function drawSlot(slot, flash) {
      var value = SLOT_VALUES[slot];
      var colour = value < 0 ? C[4] : blend(C[0], C[5], 0.4 + 0.2 * value);
      var left = px(slot - 4.5) + SLOT_GAP, right = px(slot - 3.5) - SLOT_GAP;
      var top = py(SLOT_TOP_Y), bottom = py(SLOT_BOTTOM_Y);
      fill(blend(C[0], colour, 0.25 + 0.75 * flash));
      roundRect(left, top, right, bottom, 8);
      fill(blend(value < 0 ? C[4] : C[5], C[0], flash));
      font(24);
      ctx.textAlign = "center";
      ctx.textBaseline = "middle";
      ctx.fillText(value > 0 ? "+" + value : String(value), (left + right) / 2, (top + bottom) / 2 + 1);
    }

    function draw() {
      fill(C[0]);
      ctx.fillRect(0, 0, W, H);

      fill(C[1]);
      for (var row = 0; row < ROWS; row++) {
        for (var index = 0; index <= row; index++) circle(px(index - row / 2), py(row), PIN_RADIUS);
      }
      for (var slot = 0; slot < SLOTS; slot++) {
        var since = hits[slot] === undefined ? -1 : frame - hits[slot];
        drawSlot(slot, since >= 0 && since < FLASH_FRAMES ? 1 - since / FLASH_FRAMES : 0);
      }

      // the glowing pin, and its burst when a ball takes it
      var bx = px(bonus.index - bonus.row / 2), by = py(bonus.row);
      ctx.textAlign = "center";
      ctx.textBaseline = "alphabetic";
      if (!bonus.taken) {
        fill(C[2], 70 / 255); circle(bx, by, PIN_RADIUS * 2.6);
        fill(C[2]); circle(bx, by, PIN_RADIUS * 1.5);
      } else if (frame - bonus.at < BONUS_FLASH_FRAMES) {
        var pr = (frame - bonus.at) / BONUS_FLASH_FRAMES;
        fill(C[2], 200 / 255 * (1 - pr)); circle(bx, by, PIN_RADIUS * (1.5 + 3 * pr));
        fill(C[2], 1 - pr * pr); font(22);
        ctx.fillText("+1", bx, by - 14 - 30 * pr);
      }

      balls.forEach(function (ball) {
        if (frame < ball.start) return;
        var pos = position(ball);
        fill(C[2], ball.spent ? 110 / 255 : 1);
        circle(px(pos[0]), py(pos[1]), BALL_RADIUS);
      });

      popups.forEach(function (pp) {
        var pr = (frame - pp.start) / POPUP_FRAMES, value = SLOT_VALUES[pp.slot], m = pp.points / value;
        fill(C[3], 1 - pr * pr); font(22);
        ctx.fillText(m > 1 ? value + " x" + m : "+" + value, px(pp.slot - 4), py(SLOT_TOP_Y) - 8 - 34 * pr);
      });
      ctx.globalAlpha = 1;

      drawStatus();
      if (phase !== "playing") drawBanner();
    }

    function drawStatus() {
      ctx.textBaseline = "alphabetic";
      fill(C[3]); font(30); ctx.textAlign = "left";
      ctx.fillText(String(level), MARGIN, 41);

      var dotsX = MARGIN + ctx.measureText(String(level)).width + 18;
      var dots = Math.max(BALLS_PER_LEVEL, ballsLeft);
      for (var b = 0; b < dots; b++) {
        fill(b < ballsLeft ? C[2] : blend(C[0], C[1], 0.45));
        circle(dotsX + b * 17, 31, 6);
        if (b >= BALLS_PER_LEVEL) {
          ctx.strokeStyle = ctx.fillStyle; ctx.lineWidth = 2;
          ctx.beginPath(); ctx.arc(dotsX + b * 17, 31, 8.5, 0, Math.PI * 2); ctx.stroke();
        }
      }

      var goal = target(level), progress = Math.min(1, score / goal);
      fill(blend(C[0], C[1], 0.35));
      roundRect(MARGIN, 54, MARGIN + BAR_WIDTH, 64, 5);
      if (progress > 0) {
        fill(progress >= 1 ? C[5] : C[2]);
        roundRect(MARGIN, 54, MARGIN + Math.max(10, BAR_WIDTH * progress), 64, 5);
      }
      fill(C[3]); font(20);
      ctx.fillText(score + " / " + goal, MARGIN, 88);

      var shownBest = String(Math.max(best, level));
      font(30); ctx.textAlign = "right";
      ctx.fillText(shownBest, W - MARGIN, 41);
      ctx.save();
      ctx.translate(W - MARGIN - ctx.measureText(shownBest).width - 18, 30);
      fill(C[2]); ctx.fill(star);
      ctx.restore();

      for (var life = 0; life < START_LIVES; life++) {
        ctx.save();
        ctx.translate(W - MARGIN - 10 - life * 26, 64);
        fill(life < lives ? C[4] : blend(C[0], C[1], 0.45));
        ctx.fill(heart);
        ctx.restore();
      }

      var m = liveMultiplier();
      if (m > 1) {
        fill(C[2]); font(22 + 2 * Math.min(m, 10));
        ctx.fillText("x" + m, W - MARGIN, 112);
      }
    }

    function drawBanner() {
      var won = phase === "won";
      var text = won ? "LEVEL " + (level + 1) : phase === "over" ? "GAME OVER" : "TRY AGAIN";
      font(46);
      var half = ctx.measureText(text).width / 2 + 28, middle = H * 0.46;
      fill(won ? C[5] : C[4]);
      roundRect(W / 2 - half, middle - 42, W / 2 + half, middle + 42, 20);
      fill(C[0]);
      ctx.textAlign = "center";
      ctx.textBaseline = "middle";
      ctx.fillText(text, W / 2, middle + 2);
    }

    // ---- loop: whole 40 ms frames, drawn on animation frames

    var running = false, clock = 0, last = 0;
    function tick(now) {
      clock += Math.min(200, now - last);
      last = now;
      while (clock >= FRAME_MS) { clock -= FRAME_MS; step(); }
      draw();
      if (busy()) requestAnimationFrame(tick); else running = false;
    }
    function run() {
      if (running) return;
      running = true;
      clock = FRAME_MS;
      last = performance.now();
      requestAnimationFrame(tick);
    }

    button.addEventListener("click", function () {
      if (phase !== "playing" || ballsLeft === 0) return;
      ballsLeft--;
      chainSizes.push(1);
      launch(chainSizes.length - 1, false);
      run();
    });

    if (document.fonts && document.fonts.ready) document.fonts.ready.then(draw);
    draw();
  })();

  // ------------------------------------------------------------------ maze
  // Maze (the winding style) and MazeRenderer: 15 by 15 cells of 40 pixels,
  // walls of 6, and a route drawn at 30 cells a second.

  (function () {
    var widget = document.getElementById("maze");
    var canvas = widget.querySelector("canvas");
    var ctx = canvas.getContext("2d");
    var N = 15, CELL = 40, WALL = 6, ORIGIN = WALL / 2, SPEED = 30;
    var BG = "#10141F", WALLS = "#DCE3F2", PATH = "#4FC3F7", START = "#66BB6A", EXIT = "#EF5350";
    var open, route, anim = 0;

    function build() {
      // open[c] holds the sides of cell c that have no wall: 1 north, 2 east, 4 south, 8 west
      open = new Array(N * N).fill(0);
      var seen = new Array(N * N).fill(false), stack = [0];
      var dirs = [[0, -1, 1, 4], [1, 0, 2, 8], [0, 1, 4, 1], [-1, 0, 8, 2]];
      seen[0] = true;
      while (stack.length) {
        var c = stack[stack.length - 1], x = c % N, y = Math.floor(c / N);
        var options = dirs.filter(function (d) {
          var nx = x + d[0], ny = y + d[1];
          return nx >= 0 && ny >= 0 && nx < N && ny < N && !seen[ny * N + nx];
        });
        if (!options.length) { stack.pop(); continue; }
        var d = options[Math.floor(Math.random() * options.length)], n = (y + d[1]) * N + x + d[0];
        open[c] |= d[2];
        open[n] |= d[3];
        seen[n] = true;
        stack.push(n);
      }
      var from = new Array(N * N).fill(-1), queue = [0], exit = N * N - 1;
      from[0] = 0;
      while (queue.length && from[exit] < 0) {
        var cell = queue.shift();
        dirs.forEach(function (dd) {
          var nb = cell + dd[0] + dd[1] * N;
          if ((open[cell] & dd[2]) && from[nb] < 0) { from[nb] = cell; queue.push(nb); }
        });
      }
      route = [];
      for (var at = exit; at !== 0; at = from[at]) route.unshift(at);
      route.unshift(0);
    }

    function cx(c) { return ORIGIN + (c % N + 0.5) * CELL; }
    function cy(c) { return ORIGIN + (Math.floor(c / N) + 0.5) * CELL; }

    // progress runs from 0 (empty maze) to route.length - 1 (whole route)
    function draw(progress) {
      ctx.fillStyle = BG;
      ctx.fillRect(0, 0, canvas.width, canvas.height);

      ctx.fillStyle = START;
      ctx.beginPath(); ctx.arc(cx(0), cy(0), CELL * 0.3, 0, Math.PI * 2); ctx.fill();
      ctx.fillStyle = EXIT;
      ctx.beginPath(); ctx.arc(cx(N * N - 1), cy(N * N - 1), CELL * 0.3, 0, Math.PI * 2); ctx.fill();

      ctx.strokeStyle = WALLS;
      ctx.lineWidth = WALL;
      ctx.lineCap = "square";
      ctx.strokeRect(ORIGIN, ORIGIN, N * CELL, N * CELL);
      ctx.beginPath();
      for (var c = 0; c < N * N; c++) {
        var right = ORIGIN + (c % N + 1) * CELL, bottom = ORIGIN + (Math.floor(c / N) + 1) * CELL;
        if (c % N !== N - 1 && !(open[c] & 2)) { ctx.moveTo(right, bottom - CELL); ctx.lineTo(right, bottom); }
        if (Math.floor(c / N) !== N - 1 && !(open[c] & 4)) { ctx.moveTo(right - CELL, bottom); ctx.lineTo(right, bottom); }
      }
      ctx.stroke();

      if (progress <= 0) return;
      var full = Math.min(Math.floor(progress), route.length - 2), part = progress - full;
      var a = route[full], b = route[full + 1];
      var tipX = cx(a) + (cx(b) - cx(a)) * part, tipY = cy(a) + (cy(b) - cy(a)) * part;
      ctx.strokeStyle = PATH;
      ctx.lineWidth = CELL / 3;
      ctx.lineCap = "round";
      ctx.lineJoin = "round";
      ctx.beginPath();
      ctx.moveTo(cx(route[0]), cy(route[0]));
      for (var i = 1; i <= full; i++) ctx.lineTo(cx(route[i]), cy(route[i]));
      ctx.lineTo(tipX, tipY);
      ctx.stroke();
      ctx.fillStyle = PATH;
      ctx.beginPath(); ctx.arc(tipX, tipY, CELL / 3 * 0.8, 0, Math.PI * 2); ctx.fill();
    }

    function solve() {
      cancelAnimationFrame(anim);
      var end = route.length - 1;
      if (reduceMotion) { draw(end); return; }
      var started = performance.now();
      (function frameFn(now) {
        var progress = Math.min(end, (now - started) / 1000 * SPEED);
        draw(progress);
        if (progress < end) anim = requestAnimationFrame(frameFn);
      })(started);
    }

    widget.querySelector('[data-maze="new"]').addEventListener("click", function () {
      cancelAnimationFrame(anim);
      build();
      draw(0);
    });
    widget.querySelector('[data-maze="solve"]').addEventListener("click", solve);

    build();
    draw(0);
  })();

  // ------------------------------------------------------- generated image
  // NoiseRenderer with the app's defaults: value noise, scale 5, 4 octaves,
  // a 10-second loop of 180 frames that morphs through 4 noise maps while
  // the warp flows around twice. Tap for a new seed and palette.

  (function () {
    var button = document.getElementById("gen");
    var canvas = button.querySelector("canvas");
    var ctx = canvas.getContext("2d");
    var SIZE = 192, LUT_SIZE = 1024, SCALE = 5, OCTAVES = 4;
    var FRAME_COUNT = 180, FRAME_MS = 56, MORPH_COUNT = 4, WARP_TURNS = 2;
    var PALETTES = [
      [[15, 32, 64], [34, 139, 167], [244, 210, 122], [250, 245, 224]],
      [[32, 20, 55], [92, 45, 145], [229, 91, 143], [255, 194, 112]],
      [[12, 48, 44], [42, 120, 75], [157, 193, 92], [239, 230, 156]],
      [[8, 18, 38], [25, 70, 145], [67, 170, 210], [220, 245, 255]],
      [[48, 8, 18], [135, 24, 42], [238, 87, 49], [255, 211, 105]],
      [[28, 12, 52], [83, 35, 125], [164, 77, 181], [242, 170, 211]],
      [[20, 28, 24], [84, 69, 45], [164, 125, 69], [238, 218, 157]],
      [[24, 12, 8], [91, 46, 24], [188, 103, 43], [255, 205, 108]],
      [[10, 35, 35], [16, 112, 105], [53, 190, 154], [190, 245, 194]]
    ];

    var image = ctx.createImageData(SIZE, SIZE);
    var noise = new Float32Array(SIZE * SIZE);
    var sampleX = new Float32Array(SIZE), sampleY = new Float32Array(SIZE);
    var warpX = new Float32Array(SIZE), warpY = new Float32Array(SIZE);
    var lut = new Uint8ClampedArray(LUT_SIZE * 3);
    var seed, offsetX, offsetY, palette = -1, frameNo = 0;

    function pick() {
      var next;
      do { next = Math.floor(Math.random() * PALETTES.length); } while (next === palette);
      palette = next;
      var cols = PALETTES[palette];
      for (var i = 0; i < LUT_SIZE; i++) {
        var pos = i / (LUT_SIZE - 1) * (cols.length - 1);
        var idx = Math.min(cols.length - 2, Math.floor(pos)), t = pos - idx;
        for (var k = 0; k < 3; k++) lut[i * 3 + k] = cols[idx][k] + (cols[idx + 1][k] - cols[idx][k]) * t;
      }
      seed = (Math.random() * 0x7fffffff) | 0;
      offsetX = Math.random() * 10000;
      offsetY = Math.random() * 10000;
    }

    function hash(x, y, s) {
      var h = (s + Math.imul(x, 374761393) + Math.imul(y, 668265263)) | 0;
      h = Math.imul(h ^ (h >>> 13), 1274126177);
      h ^= h >>> 16;
      return (h >>> 0) / 4294967295;
    }

    // a corner's value at this morph position: every corner has its own head
    // start, so parts of the image change while others settle
    var morph = 0;
    function corner(x, y, s) {
      var time = morph + hash(x, y, s ^ 0x5DEECE6) * MORPH_COUNT;
      var stepNo = Math.floor(time), amount = time - stepNo;
      amount = amount * amount * (3 - 2 * amount);
      var from = hash(x, y, s + (stepNo % MORPH_COUNT) * 104729);
      var to = hash(x, y, s + ((stepNo + 1) % MORPH_COUNT) * 104729);
      return from + (to - from) * amount;
    }

    function render(progress) {
      var phase = progress * WARP_TURNS * Math.PI * 2;
      morph = progress * MORPH_COUNT;
      noise.fill(0);
      var total = 0;
      for (var octave = 0; octave < OCTAVES; octave++) {
        var grid = Math.max(2, Math.round(SCALE * (1 << octave)));
        var weight = 1 / (1 << octave), s = (seed + octave * 7919) | 0;
        var warp = 0.18 + octave * 0.04;
        total += weight;
        for (var i = 0; i < SIZE; i++) {
          sampleX[i] = i / SIZE * grid + offsetX;
          sampleY[i] = i / SIZE * grid + offsetY;
          warpX[i] = Math.sin(sampleY[i] * 1.35 + phase) * warp;
          warpY[i] = Math.cos(sampleX[i] * 1.15 - phase) * warp;
        }
        var cX = NaN, cY = NaN, tl = 0, tr = 0, bl = 0, br = 0;
        for (var y = 0; y < SIZE; y++) {
          var rowWarp = warpX[y], rowSample = sampleY[y], rowStart = y * SIZE;
          for (var x = 0; x < SIZE; x++) {
            var wx = sampleX[x] + rowWarp, wy = rowSample + warpY[x];
            var x0 = Math.floor(wx), y0 = Math.floor(wy);
            var tx = wx - x0, ty = wy - y0;
            tx = tx * tx * (3 - 2 * tx);
            ty = ty * ty * (3 - 2 * ty);
            if (x0 !== cX || y0 !== cY) {
              tl = corner(x0, y0, s); tr = corner(x0 + 1, y0, s);
              bl = corner(x0, y0 + 1, s); br = corner(x0 + 1, y0 + 1, s);
              cX = x0; cY = y0;
            }
            var top = tl + (tr - tl) * tx, bottom = bl + (br - bl) * tx;
            noise[rowStart + x] += (top + (bottom - top) * ty) * weight;
          }
        }
      }
      var toLut = (LUT_SIZE - 1) / total, data = image.data;
      for (var p = 0; p < noise.length; p++) {
        var index = Math.max(0, Math.min(LUT_SIZE - 1, (noise[p] * toLut + 0.5) | 0)) * 3;
        data[p * 4] = lut[index];
        data[p * 4 + 1] = lut[index + 1];
        data[p * 4 + 2] = lut[index + 2];
        data[p * 4 + 3] = 255;
      }
      ctx.putImageData(image, 0, 0);
    }

    var timer = 0;
    function start() {
      if (timer || reduceMotion) return;
      timer = setInterval(function () {
        frameNo = (frameNo + 1) % FRAME_COUNT;
        render(frameNo / FRAME_COUNT);
      }, FRAME_MS);
    }
    function stop() { clearInterval(timer); timer = 0; }

    pick();
    render(0);
    whileVisible(canvas, start, stop);
    button.addEventListener("click", function () { pick(); render(frameNo / FRAME_COUNT); });
  })();
})();

/* うさぎ鉄道 URMS 表示テーマ (ライト / ダーク) 切替 - jQuery 非依存, <head> で同期読込 */
(function (w, d) {
    'use strict';
    var KEY = 'urms.theme', DARK = 'theme-dark', root = d.documentElement;

    function saved() {
        try { return w.localStorage ? w.localStorage.getItem(KEY) : null; } catch (e) { return null; }
    }
    function prefersDark() {
        return !!(w.matchMedia && w.matchMedia('(prefers-color-scheme: dark)').matches);
    }
    function isDark() {
        return (' ' + root.className + ' ').indexOf(' ' + DARK + ' ') >= 0;
    }
    function apply(dark) {
        var cls = (' ' + root.className + ' ').replace(' ' + DARK + ' ', ' ');
        root.className = (dark ? cls + DARK : cls).replace(/^\s+|\s+$/g, '');
        var btn = d.getElementById('theme-toggle');
        if (btn) {
            btn.innerHTML = dark ? '&#9728; ライト表示' : '&#9790; ダーク表示';
            btn.setAttribute('aria-pressed', dark ? 'true' : 'false');
        }
    }

    var s = saved();
    apply(s ? s === 'dark' : prefersDark());

    function onReady() {
        apply(isDark());
        var btn = d.getElementById('theme-toggle');
        if (!btn) { return; }
        btn.onclick = function () {
            var dark = !isDark();
            apply(dark);
            try { w.localStorage.setItem(KEY, dark ? 'dark' : 'light'); } catch (e) { /* 保存不可時は画面内のみ */ }
            return false;
        };
    }
    if (d.addEventListener) {
        d.addEventListener('DOMContentLoaded', onReady, false);
    } else {
        w.attachEvent('onload', onReady);
    }
})(window, document);

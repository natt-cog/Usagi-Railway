/* うさぎ鉄道 URMS 端末共通スクリプト (jQuery 1.12 / IE8 対応) */
(function ($) {
    'use strict';

    // 操作前の確認ダイアログ (き電停止・復電など)
    $(document).on('submit', 'form.js-confirm', function () {
        var msg = $(this).attr('data-confirm') || '実行しますか？';
        return window.confirm(msg);
    });

    // 製造番号: 全角→半角, 大文字化
    function normalizeSerial(input) {
        var v = $(input).val().replace(/[０-９Ａ-Ｚａ-ｚ－]/g, function (c) { return String.fromCharCode(c.charCodeAt(0) - 0xFEE0); });
        v = $.trim(v).toUpperCase();
        $(input).val(v);
        return v;
    }
    $(document).on('blur', 'input.serial', function () {
        normalizeSerial(this);
    });

    // 故障登録: 製造番号から搭載位置を照会 (REST API)
    $(document).on('change', 'form.failure input.serial', function () {
        var target = $('#serial-lookup');
        var serial = normalizeSerial(this);
        if (serial.length < 6) { target.text(''); return; }
        var ctx = $('#nav a:first').attr('href').replace(/\/dashboard$/, '');
        $.ajax({
            url: ctx + '/api/equipment/' + encodeURIComponent(serial),
            dataType: 'json',
            success: function (e) {
                target.text(e.typeLabel + ' ' + e.model + ' / ' + (e.formationNo ? e.formationNo + ' 編成 ' + e.carNo + ' 号車' : '予備品 (未搭載)'));
            },
            error: function () { target.text('該当機器なし'); }
        });
    });

    // 簡易ソート (テーブルヘッダクリック)
    $(document).on('click', 'table.sortable th', function () {
        var th = $(this), table = th.closest('table'), idx = th.index();
        var rows = table.find('tbody tr').get();
        var asc = !th.hasClass('asc');
        rows.sort(function (a, b) {
            var x = $(a).children('td').eq(idx).text().replace(/,/g, '');
            var y = $(b).children('td').eq(idx).text().replace(/,/g, '');
            var nx = parseFloat(x), ny = parseFloat(y);
            if (!isNaN(nx) && !isNaN(ny)) { return asc ? nx - ny : ny - nx; }
            return asc ? x.localeCompare(y) : y.localeCompare(x);
        });
        $.each(rows, function (i, r) { table.children('tbody').append(r); });
        table.find('th').removeClass('asc desc');
        th.addClass(asc ? 'asc' : 'desc');
    });

    // 警報一覧: 60 秒ごとに自動更新 (指令所常時表示用)
    if ($('body').attr('data-autorefresh')) {
        window.setTimeout(function () { window.location.reload(); }, 60000);
    }
})(jQuery);

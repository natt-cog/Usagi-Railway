<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="電力日報" />
<%@ include file="../common/header.jspf" %>
<h1>変電所別 電力日報 <small>${date}</small></h1>
<div class="panel">
  <form method="get" class="inline-form">
    <label>計測日 <input type="text" name="date" value="${date}" size="10"></label>
    <button type="submit">表示</button>
    <a class="button" href="${ctx}/power/daily.dat?date=${date}">帳票ファイル (DAILY.DAT) ダウンロード</a>
  </form>
  <table class="grid" id="daily">
    <thead><tr><th>変電所</th><th>有効件数</th><th>最低電圧 [V]</th><th>平均電圧 [V]</th><th>最大電流 [A]</th><th>電力量 [kWh]</th><th>電圧低下</th><th>過電流</th><th>欠測</th></tr></thead>
    <tbody>
    <c:set var="total" value="${0}" />
    <c:forEach var="r" items="${rows}">
      <tr>
        <td>${r.substationCode} <c:out value="${r.substationName}" /></td>
        <td class="num">${r.validCount}</td>
        <td class="num"><fmt:formatNumber value="${r.minVoltageV}" /></td>
        <td class="num"><fmt:formatNumber value="${r.avgVoltageV}" /></td>
        <td class="num"><fmt:formatNumber value="${r.maxCurrentA}" /></td>
        <td class="num"><fmt:formatNumber value="${r.energyKwh}" /></td>
        <td class="num ${r.undervoltageCount gt 0 ? 'over' : ''}">${r.undervoltageCount}</td>
        <td class="num ${r.overcurrentCount gt 0 ? 'over' : ''}">${r.overcurrentCount}</td>
        <td class="num">${r.missingCount}</td>
      </tr>
      <c:set var="total" value="${total + r.energyKwh}" />
    </c:forEach>
    <c:if test="${empty rows}"><tr><td colspan="9" class="empty">計測値なし</td></tr></c:if>
    </tbody>
    <tfoot><tr><th colspan="5">合計</th><th class="num"><fmt:formatNumber value="${total}" /></th><th colspan="3"></th></tr></tfoot>
  </table>
</div>
<div class="panel">
  <h2>帳票ファイル <small>C バッチ URPWD01 と同一形式 (DAILY_YYYYMMDD.DAT)</small></h2>
  <pre class="file"><c:forEach var="l" items="${fileLines}"><c:out value="${l}" />
</c:forEach></pre>
</div>
<%@ include file="../common/footer.jspf" %>

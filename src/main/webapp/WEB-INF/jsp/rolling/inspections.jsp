<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="検査期限" />
<%@ include file="../common/header.jspf" %>
<h1>検査期限一覧 <small>基準日 ${operationDate} / 注意: 残 ${warnDays} 日以内 または 重要部後 57 万 km 以上</small></h1>
<div class="panel">
<table class="grid sortable" id="inspections">
  <thead><tr><th>編成</th><th>交番検査 期限</th><th>重要部検査 期限</th><th>全般検査 期限</th><th>次回</th><th>次回期限</th><th>残日数</th><th>走行km</th><th>判定</th></tr></thead>
  <tbody>
  <c:forEach var="d" items="${dues}">
    <tr>
      <td><a href="${ctx}/rolling/formations/${d.formationNo}">${d.formationNo}</a></td>
      <td>${d.kobanDueText}</td><td>${d.juyobuDueText}</td><td>${d.zenpanDueText}</td>
      <td>${d.nextKind.label}</td><td>${d.nextDueText}</td>
      <td class="num ${d.daysRemaining lt 0 ? 'over' : ''}">${d.daysRemaining}</td>
      <td class="num ${d.kmExceeded ? 'over' : ''}"><fmt:formatNumber value="${d.kmSinceJuyobu}" /></td>
      <td><span class="judge-${d.judge}">${d.judge.label}</span></td>
    </tr>
  </c:forEach>
  </tbody>
</table>
</div>
<div class="panel">
  <h2>検査期限ファイル <small>検査期限バッチ URINS01 と同一形式 (INSPDUE.DAT)</small></h2>
  <pre class="file"><c:forEach var="l" items="${fileLines}"><c:out value="${l}" />
</c:forEach></pre>
</div>
<%@ include file="../common/footer.jspf" %>

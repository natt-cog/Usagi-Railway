<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="編成一覧" />
<%@ include file="../common/header.jspf" %>
<h1>編成一覧</h1>
<div class="panel">
<table class="grid sortable" id="formations">
  <thead><tr><th>編成</th><th>形式</th><th>両数</th><th>所属</th><th>運用状態</th><th>総走行 [km]</th><th>重要部後 [km]</th><th>次回検査</th><th>期限</th><th>判定</th></tr></thead>
  <tbody>
  <c:forEach var="f" items="${formations}">
    <c:set var="d" value="${dues[f.formationNo]}" />
    <tr>
      <td><a href="${ctx}/rolling/formations/${f.formationNo}">${f.formationNo}</a></td>
      <td><c:out value="${f.series}" /></td>
      <td class="num">${f.carCount}</td>
      <td><c:out value="${f.depot}" /></td>
      <td class="fs-${f.status}">${f.status.label}</td>
      <td class="num"><fmt:formatNumber value="${f.totalKm}" /></td>
      <td class="num"><fmt:formatNumber value="${f.kmSinceJuyobu}" /></td>
      <td>${d.nextKind.label}</td>
      <td>${d.nextDueText}</td>
      <td><span class="judge-${d.judge}">${d.judge.label}</span></td>
    </tr>
  </c:forEach>
  </tbody>
</table>
</div>
<div class="panel">
  <h2>予備品 (未搭載)</h2>
  <table class="grid">
    <thead><tr><th>製造番号</th><th>種別</th><th>型式</th><th>メーカー</th><th>製造日</th></tr></thead>
    <tbody>
    <c:forEach var="e" items="${spares}">
      <tr><td><a href="${ctx}/rolling/equipment/${e.serialNo}">${e.serialNo}</a></td><td>${e.equipmentType.label}</td><td>${e.model}</td>
          <td><c:out value="${e.maker}" /></td><td><fmt:formatDate value="${e.manufacturedOn}" pattern="yyyy/MM/dd" /></td></tr>
    </c:forEach>
    </tbody>
  </table>
</div>
<%@ include file="../common/footer.jspf" %>

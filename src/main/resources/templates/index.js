var cmArray = [];

var cmEditables = [];

[# th:each="file : ${files}"]
	var readOnly = true;
	[# th:each="editable : ${editables}"]
		if ([# th:text="${file.name}"/] == [# th:text ="${editable}"/]) {
			readOnly = false;
		}
	[/]
	var [# th:utext="${file.name}"/] = new CodeMirror(document.getElementById([# th:text="|${file.filename}|"/]), {
		lineNumbers: true,
		matchBrackets: true,
		mode: "text/x-java",
		readOnly: readOnly
	  });
	cmArray.push([# th:utext="${file.name}"/]);
	[# th:each="editable : ${editables}"]
		if ([# th:text="${file.name}"/] == [# th:text ="${editable}"/]) {
			cmEditables.push([# th:utext="${file.name}"/]);
		}
	[/]
		
	[# th:utext="${file.name}"/].setValue([# th:text="${file.content}"/]);

[/]  

$('#tabs').bind('tabsactivate',function(e, ui) {
	var curTab = $('.ui-state-active');
	console.log(curTab.index());
	cmArray[curTab.index()].refresh();
	$(ui.newPanel).find(".cm-s-default")
});
	
	
//var mac = CodeMirror.keyMap.default == CodeMirror.keyMap.macDefault;
//CodeMirror.keyMap.default[(mac ? "Cmd" : "Ctrl") + "-Space"] = "autocomplete";

	
	

var filesArray = [];

[# th:each="file : ${files}"]
var [# th:utext="${file.name}"/] = new CodeMirror(document.getElementById([# th:text="|${file.filename}|"/]), {
	lineNumbers: true,
	matchBrackets: true,
	mode: "text/x-java",
	readOnly: [# th:text="|${file.readOnly}|"/]
  });
filesArray.push({ filename:[# th:text="|${file.filename}|"/]
				, cmEditor: [# th:utext="${file.name}"/]
				, readonly: [# th:text="|${file.readOnly}|"/]
				, fileType: [# th:text="|${file.fileType.name()}|"/]
				});

	
[# th:utext="${file.name}"/].setValue([# th:text="${file.content}"/]);

[/]  

$('#tabs').bind('tabsactivate',function(e, ui) {
	var curTab = $('.ui-state-active');
	filesArray[curTab.index()].cmEditor.refresh();
});
	
	
//var mac = CodeMirror.keyMap.default == CodeMirror.keyMap.macDefault;
//CodeMirror.keyMap.default[(mac ? "Cmd" : "Ctrl") + "-Space"] = "autocomplete";
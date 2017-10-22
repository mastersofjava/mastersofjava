var filesArray = [];
var testsArray = [];

[# th:each="file : ${files}"]

if ([# th:text="|${file.fileType.name()}|"/] == 'EDIT'  || [# th:text="|${file.fileType.name()}|"/] == 'READONLY') {

	var [# th:utext="${file.name}"/] = new CodeMirror(document.getElementById([# th:text="|${file.name}|"/]), {
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
	[# th:utext="${file.name}"/].setSize('100%', 500);
	
} else {
		
	if ([# th:text="|${file.fileType.name()}|"/] == 'TASK') {
		var x = document.getElementById([# th:text="|${file.name}|"/]);
		x.innerHTML = "<pre>" + [# th:text="|${file.content}|"/] + "</pre>";

	} else {
		testsArray.push([# th:text="|${file.name}|"/]);
		var x = document.getElementById([# th:text="|${file.name}|"/]);
		x.innerHTML = '<textarea></textarea>';
	}
}

[/]  

$('#tabs').bind('tabsactivate',function(e, ui) {
	var curTab = $('.ui-tabs-tab');
	console.log('hier');
	if (filesArray[curTab.index()] !=  null) {
		console.log('hierook');
		filesArray[curTab.index()].cmEditor.refresh();
	}
});
	
	
//var mac = CodeMirror.keyMap.default == CodeMirror.keyMap.macDefault;
//CodeMirror.keyMap.default[(mac ? "Cmd" : "Ctrl") + "-Space"] = "autocomplete";
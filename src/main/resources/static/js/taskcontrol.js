	var stompClient = null;

    function getContent() {
		var curTab = $('.ui-state-active');
		console.log(curTab.index());
		var editables = new Array();
		for(let i = 0; i < cmEditables.length; i++){
			editables.push(cmEditables[i].getValue());
		}
		return editables;
    }  
      
	function setConnected(connected) {
		document.getElementById('response').innerHTML = '';
	}

	function connect() {

		var socket = new SockJS('/control');
		stompClient = Stomp.over(socket);
		stompClient.debug = null;
		stompClient.connect({}, function(frame) {

			setConnected(true);
			console.log('Connected to control');
			
			stompClient.subscribe('/user/queue/feedback', function(messageOutput) {
				console.log("user feedback")
				//showMessageOutput(JSON.parse(messageOutput.body));
				showOutput(messageOutput.body);
			});

		});
	}

	function disconnect() {

		if (stompClient != null) {
			stompClient.disconnect();
		}

		setConnected(false);
	}

	function startTask() {
		var taskname = $( "input:checked" ).val();
		console.log(taskname);
		stompClient.send("/app/control/starttask", {}, JSON.stringify({
			'taskName' : taskname
		}));
	}

	function getSplit() {
		stompClient.send("/app/control/getsplit", {}, JSON.stringify({
			'taskName' : 'task1'
		}));
	}

	function clearAssignment() {
		var taskname = $( "input:checked" ).val();
		console.log(taskname);
		stompClient.send("/app/control/clearAssignment", {}, {});
	}
	
	function cloneAssignmentsRepo() {
		stompClient.send("/app/control/cloneAssignmentsRepo", {}, {});
	}
	
	function showMessageOutput(messageOutput) {
		console.log("show");
		var response = document.getElementById('response');
		response.insertBefore(document.createElement('hr'), response.firstElementChild);
		var p = document.createElement('p');
		p.appendChild(document.createTextNode(messageOutput.elapsedTime));
		response.insertBefore(p, response.firstElementChild);
	}
	
	function showOutput(messageOutput) {
		var response = document.getElementById('response');
		var p = document.createElement('p');
		p.style.wordWrap = 'break-word';
		p.appendChild(document.createTextNode(messageOutput));
		response.appendChild(p);
	}


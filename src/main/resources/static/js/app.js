	var stompClient = null;

	function init() {
		connect();
		connectControl();
		connectTaskTime();
	}
	
    function getContent() {
		var curTab = $('.ui-state-active');
		console.log(curTab.index());
		var editables = [];
		for(let i = 0; i < filesArray.length; i++){
			console.log('type:' + filesArray[i].fileType)
			if (!filesArray[i].readonly &&  filesArray[i].fileType === 'EDIT') { 
				console.log('in');
				var file = {filename: filesArray[i].filename, content: filesArray[i].cmEditor.getValue()}
				editables.push(file);				
			}
		}
		
		return editables;
    }  
      
	function connect() {

		var socket = new SockJS('/submit');
		stompClient = Stomp.over(socket);
		stompClient.debug = null;
		stompClient.connect({}, function(frame) {
			document.getElementById('status').innerHTML = 'connected';
			console.log('Connected');
			stompClient.subscribe('/user/queue/feedback', function(messageOutput) {
				console.log("user feedback");
				var message = JSON.parse(messageOutput.body);
				var response = document.getElementById('response');
				response.insertBefore(document.createElement('hr'), response.firstElementChild);
				var p = document.createElement('p');
				p.appendChild(document.createTextNode(message.time));
				response.insertBefore(p, response.firstElementChild);
				
				var pre = document.createElement('pre');
				pre.appendChild(document.createTextNode(message.text));
				response.insertBefore(pre, response.firstElementChild);
				
				p = document.createElement('p');
				p.appendChild(document.createTextNode(message.team + ": "));
				response.insertBefore(p, response.firstElementChild);
			});

		});
	}

	function connectControl() {

		var socket = new SockJS('/control');
		var stompClientControl = Stomp.over(socket);
		stompClientControl.debug = null;
		stompClientControl.connect({}, function(frame) {
			console.log('Connected to control');
			stompClientControl.subscribe('/queue/start', function(messageOutput) {
				console.log("/queue/start")
				window.location.reload();
			});

		});
	}
	
	function connectTaskTime() {

		var socket = new SockJS('/control');
		stompClientTaskTime = Stomp.over(socket);
		stompClientTaskTime.debug = null;
		stompClientTaskTime.connect({}, function(frame) {

			console.log('Connected to tasktime');
			stompClientTaskTime.subscribe('/queue/time', function(taskTimeMessage) {
				var message = JSON.parse(taskTimeMessage.body);
				var p = document.getElementById('tasktime');
				p.innerHTML = message.remainingTime;
			});

		});
	}	
		
	function compile() {
		stompClient.send("/app/submit/compile", {}, JSON.stringify({
			'team' : 'team1',
			'source' :  getContent()
		}));
	}

	function test() { 
		var tests = $("input[name='test']:checked").val();
		stompClient.send("/app/submit/test", {}, JSON.stringify({
			'team' : 'team1',
			'source' : getContent(),
			'tests' : tests
		}));
	}

	function submit() {
		stompClient.send("/app/submit/submit", {}, JSON.stringify({
			'team' : 'team1',
			'source' : getContent()
		}));
	}
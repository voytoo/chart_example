angular.module("javarelatedApp", [])
.controller('GraphController', function ($scope, $http, $interval) {

	$scope.frequency = 1;
	$scope.interval = 60;
	
	$scope.chart = nv.models.multiBarChart();
	$scope.chart.xAxis.tickFormat(d3.format(',f'));
	$scope.chart.yAxis.tickFormat(d3.format(',f'));
	$scope.chart.delay(0);
	
	nv.utils.windowResize($scope.chart.update);
	
	$scope.drawChart = function(poolName, interval) {
	
	    if (angular.isNumber(interval)) {
	
	        //numbers greater than 1000 does not make sense ... the graph is unreadable
	        if (interval > 1000) {
	            interval = 1000;
	        }
	
	        $http.get('rest/graph/history?interval=' + interval).success(function (data, status, headers, config) {
	
	        	var state = $scope.chart.state();
	        	
	        	//preserve state
	        	if (typeof state.disabled != 'undefined') {
		        	data[0].disabled = state.disabled[0];
		        	data[1].disabled = state.disabled[1];
		        	data[2].disabled = state.disabled[2];
	        	}
	        	
	            nv.addGraph(function () {
	
	                d3.select('#realtimeGraph svg')
	                    .datum(data)
	                    .transition().duration(0)
	                    .call($scope.chart)
	                ;
	
	                return $scope.chart;
	            });
	
	        }).error(function (data, status, headers, config) {
	
	        });
	    }
	};
	
	$scope.updateChart = function(interval) {
	    $scope.drawChart($scope.poolName, interval);
	};
	
	$scope.updateFrequency = function(frequency) {
	
	    if (angular.isNumber(frequency) && frequency > 0 && frequency < 10000) {
	        //destroy current timer
	        $interval.cancel($scope.chartTimer);
	
	        //create new timer with new frequency
	        $scope.chartTimer = $interval(
	            function () {
	                $scope.updateChart($scope.interval);
	            },
	            frequency * 1000
	        );
	    }
	};
	
	//start chart timer
	$scope.chartTimer = $interval(
	    function() {
	        $scope.updateChart($scope.interval);
	    },
	    $scope.frequency * 1000
	);
	
	$scope.$on(
	    "$destroy",
	    function( event ) {
	        $interval.cancel($scope.chartTimer);
	    }
	);
	
	$scope.updateChart($scope.interval);
});

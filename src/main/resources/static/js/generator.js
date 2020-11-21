$(function () {
    var lastsel;
    $("#jqGrid").jqGrid({
        url: 'sys/generator/list',
        datatype: "json",
        colModel: [			
			{ label: '表名', name: 'tableName', width: 100, key: true },
			// { label: 'Engine', name: 'engine', width: 70},
			{ label: '表备注', name: 'tableComment', width: 100 },
			// { label: '创建时间', name: 'createTime', width: 100 },
            { label: '模块名', name: 'moduleName', cellEdit:true, editable : true, width: 100 },
            { label: '请求路径', name: 'restPath', cellEdit:true, editable : true, width: 200 }
        ],
        onCellSelect : function(id, iCol) {
           if (iCol === 4 || iCol === 5) {
               $('#jqGrid').jqGrid('restoreRow', lastsel);
               $('#jqGrid').jqGrid('editRow', id, true, null, null, "clientArray");
                lastsel = id;
            }
            return false;
        },
		viewrecords: true,
        height: 327,
        rowNum: 10,
		rowList : [10,30,50,100,200],
        rownumbers: true, 
        rownumWidth: 25, 
        autowidth:true,
        multiselect: true,
        beforeSelectRow: function (rowid, e) {
            var $myGrid = $(this),
            i = $.jgrid.getCellIndex($(e.target).closest('td')[0]),
            cm = $myGrid.jqGrid('getGridParam', 'colModel');
            return (cm[i].name === 'cb');
        },
        pager: "#jqGridPager",
        jsonReader : {
            root: "page.list",
            page: "page.currPage",
            total: "page.totalPage",
            records: "page.totalCount"
        },
        prmNames : {
            page:"page", 
            rows:"limit", 
            order: "order"
        },
        gridComplete:function(){
        	//隐藏grid底部滚动条
        	$("#jqGrid").closest(".ui-jqgrid-bdiv").css({ "overflow-x" : "hidden" });
        }
    });
});

var vm = new Vue({
	el:'#rrapp',
	data:{
		q:{
			tableName: null
		}
	},
	methods: {
		query: function () {
			$("#jqGrid").jqGrid('setGridParam',{ 
                postData:{'tableName': vm.q.tableName},
                page:1 
            }).trigger("reloadGrid");
		},
		generator: function() {
            var tableNames = getSelectedRows();
            if(tableNames == null){
                return ;
            }

            var sendData = [];

            for (var i=0; i< tableNames.length; i ++) {
               var itemData = $("#jqGrid").jqGrid("getRowData",tableNames[i]);

               var sendItem = {
                   tableName: itemData.tableName,
                   moduleName: itemData.moduleName,
                   restPath: itemData.restPath
               }
                sendData.push(sendItem);
            }

            var sendDataStr = JSON.stringify(sendData);
            var urlparam = encodeURI("tables=" + sendDataStr);
            location.href = "sys/generator/code?" + urlparam;
		}
	}
});


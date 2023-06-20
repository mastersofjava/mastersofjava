{{/*
Define the full name
*/}}
{{- define "postgres.fullname" }}
{{- printf "%s-%s" .Release.Name "postgres" }}
{{- end }}

{{- define "postgres.service.name" }}
{{- printf "%s-%s" .Release.Name "postgres" }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "postgres.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "postgres.labels" -}}
helm.sh/chart: {{ include "postgres.chart" . }}
{{ include "postgres.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/component: database
app.kubernetes.io/part-of: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "postgres.selectorLabels" -}}
app.kubernetes.io/name: {{ .Chart.Name }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "postgres.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "postgres.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{- define "postgres.haveDatabases" -}}
{{- if .Values.global.postgres.enabled -}}
    {{- if and .Values.global.iam .Values.global.controller -}}
    true
    {{- else if gt ( len .Values.databases) 0 -}}
    true
    {{- else -}}
    false
    {{- end -}}
{{- else }}
false
{{- end -}}
{{- end -}}

{{- define "postgres.databases" -}}
{{- if .Values.global.postgres.enabled -}}
    {{- if and .Values.global.iam .Values.global.controller -}}
    {{- printf "%s:%s:%s,%s:%s:%s"
        (default "iam" .Values.global.iam.database.name)
        (default "iam" .Values.global.iam.database.username)
        (default "iam" .Values.global.iam.database.password)
        (default "controller" .Values.global.controller.database.name)
        (default "controller" .Values.global.controller.database.username)
        (default "controller" .Values.global.controller.database.password) -}}
    {{- else if gt ( len .Values.databases) 0 -}}
        {{- $list := list -}}
        {{- range $v := .Values.databases -}}
        {{- $list = append $list ( printf "%s:%s:%s" $v.name $v.username $v.password) -}}
        {{- end -}}
        {{- join "," $list -}}
    {{- end -}}
{{- end -}}
{{- end -}}
$MavenVersion = "3.9.6"
$MavenUrl = "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$MavenVersion/apache-maven-$MavenVersion-bin.zip"
$MavenZip = "apache-maven-$MavenVersion-bin.zip"
$MavenDir = "apache-maven-$MavenVersion"

if (-not (Test-Path $MavenDir)) {
    Write-Host "Downloading Maven $MavenVersion..."
    Invoke-WebRequest -Uri $MavenUrl -OutFile $MavenZip
    
    Write-Host "Extracting Maven..."
    Expand-Archive -Path $MavenZip -DestinationPath . -Force
    
    Write-Host "Cleaning up zip file..."
    Remove-Item $MavenZip
    
    Write-Host "Maven setup complete."
} else {
    Write-Host "Maven already exists in $MavenDir."
}

$MvnCmd = Join-Path $PWD "$MavenDir\bin\mvn.cmd"
Write-Host "You can now run Maven using: $MvnCmd"

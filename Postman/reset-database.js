function resetDatabase() {
    pm.sendRequest({
        url: 'http://localhost:8080/test/reset',
        method: 'POST',
        header: {
            'Content-Type': 'application/json'
        }
    }, function (err, response) {
        if (err) {
            console.log('Error resetting database:', err);
        } else {
            console.log('Database reset successfully. Status:', response.code);
        }
    });
}

resetDatabase();
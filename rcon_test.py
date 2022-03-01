from rcon.battleye import Client

with Client('34.244.97.157', 2305, passwd='sOmeP4ss4life') as client:
    response = client.run('players')

print(response)